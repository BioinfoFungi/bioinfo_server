package com.wangyang.bioinfo.service.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.wangyang.bioinfo.core.KeyLock;
import com.wangyang.bioinfo.handle.FileHandlers;
import com.wangyang.bioinfo.pojo.dto.Metadata;
import com.wangyang.bioinfo.pojo.dto.MetadataGroup;
import com.wangyang.bioinfo.pojo.dto.TaskDto;
import com.wangyang.bioinfo.pojo.entity.base.BaseEntity;
import com.wangyang.bioinfo.pojo.enums.CrudType;
import com.wangyang.bioinfo.pojo.enums.FileLocation;
import com.wangyang.bioinfo.pojo.support.UploadResult;
import com.wangyang.bioinfo.service.base.ICrudService;
import com.wangyang.bioinfo.task.ICodeResult;
import com.wangyang.bioinfo.pojo.entity.CancerStudy;
import com.wangyang.bioinfo.pojo.entity.Task;
import com.wangyang.bioinfo.pojo.authorize.User;
import com.wangyang.bioinfo.pojo.entity.base.BaseFile;
import com.wangyang.bioinfo.pojo.enums.TaskStatus;
import com.wangyang.bioinfo.pojo.enums.TaskType;
import com.wangyang.bioinfo.pojo.entity.Code;
import com.wangyang.bioinfo.pojo.param.TaskParam;
import com.wangyang.bioinfo.pojo.param.TaskQuery;
import com.wangyang.bioinfo.repository.task.TaskRepository;
import com.wangyang.bioinfo.service.*;
import com.wangyang.bioinfo.service.base.AbstractCrudService;
import com.wangyang.bioinfo.util.*;
import joinery.DataFrame;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wangyang
 * @date 2021/7/24
 */
@Service
@Slf4j
public class TaskServiceImpl extends AbstractCrudService<Task,Integer>
        implements  ITaskService{

    @Value("${bioinfo.workDir}")
    private String workDir;

    private final TaskRepository taskRepository;
    private final ICancerStudyService cancerStudyService;
    private final ICodeService codeService;
    private final IAsyncService asyncService;
    private final IOrganizeFileService organizeFileService;
    private final IAnnotationService annotationService;
    private final ApplicationContext applicationContext;
    private final KeyLock<String> lock;
    private final ThreadPoolTaskExecutor executorService;
    private final FileHandlers fileHandlers;
    //TUDO
    private int QUEUE_CAPACITY= 300;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ICancerStudyService cancerStudyService,
                           ICodeService codeService,
                           IAsyncService asyncService,
                           IOrganizeFileService organizeFileService,
                           IAnnotationService annotationService,
                           ApplicationContext applicationContext,
                           ThreadPoolTaskExecutor executorService,
                           KeyLock<String> lock,
                           FileHandlers fileHandlers) {
        super(taskRepository);
        this.taskRepository=taskRepository;
        this.cancerStudyService=cancerStudyService;
        this.codeService=codeService;
        this.asyncService=asyncService;
        this.organizeFileService=organizeFileService;
        this.annotationService=annotationService;
        this.applicationContext=applicationContext;
        this.lock=lock;
        this.executorService = executorService;
        this.fileHandlers = fileHandlers;

    }



    @Override
    public Page<Task> page(TaskQuery taskQuery, Pageable pageable) {
        return taskRepository.findAll(buildSpecByQuery(taskQuery,null),pageable);
    }



    @Override
    public List<Task> findByCodeId(Integer codeId){
        return taskRepository.findAll(new Specification<Task>() {
            @Override
            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaQuery.where(criteriaBuilder.equal(root.get("codeId"),codeId)).getRestriction();
            }
        });
    }

    public Task findByCanSIdACodeId(Integer cancerStudyId,Integer codeId,TaskType taskType){
        List<Task> tasks = taskRepository.findAll(new Specification<Task>() {
            @Override
            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaQuery.where(
                        criteriaBuilder.equal(root.get("objId"),cancerStudyId),
                        criteriaBuilder.equal(root.get("codeId"),codeId),
                        criteriaBuilder.equal(root.get("taskType"),taskType)
                ).getRestriction();
            }
        });
        return tasks.size()==0?null:tasks.get(0);
    }

    @Override
    public List<Task> delByCodeId(Integer codeId){
        List<Task> taskList = findByCodeId(codeId);
        taskRepository.deleteAll(taskList);
        return taskList;
    }


    @Override
    public List<Task> findByCanSId(Integer canSId){
        return taskRepository.findAll(new Specification<Task>() {
            @Override
            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaQuery.where(criteriaBuilder.equal(root.get("objId"),canSId)).getRestriction();
            }
        });
    }

    @Override
    public List<Task> delByCanSId(Integer canSId){
        List<Task> taskList = findByCanSId(canSId);
        taskRepository.deleteAll(taskList);
        return taskList;
    }




    public ICodeResult getCodeResult(TaskType taskType){
        Map<String, ICodeResult> beans = applicationContext.getBeansOfType(ICodeResult.class);
        ICodeResult codeResult=null;
        for (ICodeResult bean : beans.values()){
            if(bean.getTaskType().equals(taskType)){
                codeResult = bean;
                break;
            }
        }
        if(codeResult==null){
            throw new BioinfoException(taskType.name()+"的处理器没有配置！！");
        }
        return codeResult;
    }

    @Override
    public Task addTask(CrudType crudEnum, ICrudService crudService, Integer taskId, Integer id, TaskDto taskDto, Integer codeId, User user) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        Task task;
        if(taskOptional.isPresent()){
            task =taskOptional.get();
            if(runCheck(task)){
                throw new BioinfoException(task.getName() + " 已经运行或在队列中！");
            }
        }else {
            task = new Task();
        }
//        Task task = findTask(crudEnum,id,codeId);
        Code code = codeService.findById(codeId);
        BaseEntity baseEntity = crudService.findById(id);
        task.setCodeId(code.getId());
        task.setObjId(baseEntity.getId());
        task.setTaskStatus(TaskStatus.UNTRACKING);
        task.setCrudEnum(crudEnum);
//        task.setTaskType(code.getTaskType());
        task.setUserId(user.getId());
        task.setName(crudEnum.name()+"-"+code.getName());
        task.setRunMsg(Thread.currentThread().getName() + "准备开始分析！" + new Date());

//        if(taskDto.getGroups()!=null&& task.getGroups().length()>0){
//            String jsonString = JSONArray.toJSONString(taskDto.getGroups());
//            task.setGroups(jsonString);
//        }

        Map<String,String> map = taskDto.getMap();
//        List<Field> fieldList = ObjectToCollection.setConditionFieldList(baseEntity);
//        Set<String> vars = ServiceUtil.fetchProperty(fieldList, Field::getName);
//        map.put(ObjectToCollection.setConditionMap(baseEntity))
//        map.putAll(ObjectToCollection.setConditionMap(baseEntity));
        map.put("metadata",task.getMetadata());
        map.put("matrix",task.getMatrix());
        String paramJson = JSONObject.toJSONString(map);
        task.setParam(paramJson);


        task=taskRepository.save(task);
        Path path = Paths.get(workDir, "workspace", user.getUsername(), "metadata." + task.getId() + ".csv");
        task.setMetadata(path.toString());

        String metadata = taskDto.getMetadata();
        if(metadata!=null&&!metadata.equals("null")){
            Table table = Table.read().string(metadata, "json");
            int rowCount = table.rowCount();
            if(rowCount!=0){
                String content = table.write().toString("csv");
                FileUtil.writeFile(content,task.getMetadata());
                System.out.println();
                processMetadata(task,table,taskDto.getGroups());
            }
        }
        asyncService.processCancerStudy1(user,task,map,code);
        return task;
    }

    @Override
    public Task addTask(CrudType crudEnum, ICrudService<BaseEntity,Integer> crudService, Integer id, Integer codeId, User user) {
        Task task = findTask(crudEnum,id,codeId);
        Code code = codeService.findById(codeId);

        if (task==null) {
            task = new Task();
        }else {
            if(runCheck(task)){
                throw new BioinfoException(task.getName() + " 已经运行或在队列中！");
            }
        }
        BaseEntity baseEntity = crudService.findById(id);

        task.setCodeId(code.getId());
        task.setObjId(baseEntity.getId());
        task.setTaskStatus(TaskStatus.UNTRACKING);
        task.setCrudEnum(crudEnum);
//        task.setTaskType(code.getTaskType());
        task.setUserId(user.getId());
        task.setName(crudEnum.name()+"-"+code.getName());
        task.setRunMsg(Thread.currentThread().getName() + "准备开始分析！" + new Date());
        task=taskRepository.save(task);

        Map<String, String> map = ObjectToCollection.setConditionMap(baseEntity);

        //交给thread
        asyncService.processCancerStudy1(user,task,map,code);
        return task;
    }

    private Task findTask(CrudType crudEnum, Integer id, Integer codeId) {
        List<Task> tasks = taskRepository.findAll(new Specification<Task>() {
            @Override
            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaQuery.where(
                        criteriaBuilder.equal(root.get("objId"),id),
                        criteriaBuilder.equal(root.get("codeId"),codeId),
                        criteriaBuilder.equal(root.get("crudEnum"),crudEnum)
                ).getRestriction();
            }
        });
        return tasks.size()==0?null:tasks.get(0);
    }
    public  boolean runCheck(Task task){
        int activeCount = executorService.getActiveCount();
        if( activeCount!=0&& (task.getTaskStatus().equals(TaskStatus.RUNNING) || task.getTaskStatus().equals(TaskStatus.UNTRACKING))){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public Task runTask(ICrudService<BaseEntity,Integer> crudService,
                        Integer id,
                        User user) {

        Task task = findById(id);
        if(runCheck(task)){
            throw new BioinfoException(task.getName()+" 已经运行或在队列中！");
        }
        Code code = codeService.findById(task.getCodeId());
        BaseEntity baseEntity = crudService.findById(task.getObjId());
        task.setTaskStatus(TaskStatus.UNTRACKING);
        task.setRunMsg(Thread.currentThread().getName()+"准备开始分析！"+ new Date());
        task= super.save(task);
        Map<String, String> map = ObjectToCollection.setConditionMap(baseEntity);

        asyncService.processCancerStudy1(user,task,map,code);

        return task;
    }


//    @Override
//    public Task addTask(TaskParam taskParam,User user) {
//        Code code = codeService.findById(taskParam.getCodeId());
//        return addTask(code,taskParam.getObjId(),user);
//    }


//    @Override
//    public Map<String, Object> getObjMap(TaskType taskType, int objId){
//        ICodeResult codeResult = getCodeResult(taskType);
////        BaseFile baseFile = codeResult.getObj(objId);
//        return null;//codeResult.getMap(baseFile);
//    }

//    public Task addTask(Code code,int objId,User user){
//        ICodeResult codeResult = getCodeResult(code.getTaskType());
////        BaseFile baseFile = codeResult.getObj(objId);
//
////        checkRun(codeResult,code,baseFile);
//
//        Task task = findByCanSIdACodeId(objId, code.getId(),code.getTaskType());
//        if (runCheck(task)) {
//            throw new BioinfoException(task.getName() + " 已经运行或在队列中！");
//        }
//        if (task == null) {
//            task = new Task();
//        }
////        task.setObjId(baseFile.getId());
//        task.setCodeId(code.getId());
//        task.setTaskStatus(TaskStatus.UNTRACKING);
//        task.setTaskType(code.getTaskType());
//        task.setUserId(user.getId());
////        task.setName(baseFile.getFileName()+code.getName());
//        task.setRunMsg(Thread.currentThread().getName() + "准备开始分析！" + new Date());
//        task=taskRepository.save(task);
//
//        //交给thread
////        asyncService.processCancerStudy1(user,task,code,baseFile,codeResult);
//
//        return task;
//    }

    private void checkRun( ICodeResult codeResult,Code code, BaseFile baseFile) {
        Boolean checkRun =  codeResult.checkRun(code,baseFile);
        if(!checkRun){
            throw new BioinfoException(code.getName()+"不能执行"+baseFile.getId());
        }
    }

    private Boolean checkExist(ICodeResult codeResult,List<BaseFile> baseFiles) {
        Boolean aBoolean = codeResult.checkExist(baseFiles);
        return aBoolean;
    }


    @Override
    public List<CancerStudy> runByCodeId(Integer id, User user) {
        Code code = codeService.findById(id);
        List<CancerStudy> cancerStudies = cancerStudyService.findByCode(code);
        cancerStudies.forEach(cancerStudy -> {
            TaskParam taskParam = new TaskParam();
            taskParam.setCodeId(code.getId());
//            taskParam.setObjId(cancerStudy.getId());
//            addTask(taskParam,user);
        });
        return cancerStudies;
    }

    @Override
    public Task shutdownProcess(int taskId){
        Task task = asyncService.shutdownProcess(taskId);
        return task;
    }



    @Override
    public String getLogFiles(@NonNull Integer taskId,@NonNull  Integer lines){
        File file = new File(CacheStore.getValue("workDir"),"log/"+taskId+".log");
        if(!file.exists()){
            return "文件不存在！";
        }
        List<String> linesArray = new ArrayList<>();
        final StringBuilder result = new StringBuilder();
        long count = 0;
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            long length = randomAccessFile.length();
            if (length == 0L) {
                return StringUtils.EMPTY;
            } else {
                long pos = length - 1;
                while (pos > 0) {
                    pos--;
                    randomAccessFile.seek(pos);
                    if (randomAccessFile.readByte() == '\n') {
                        String line = randomAccessFile.readLine();
                        linesArray.add(new String(line.getBytes(StandardCharsets.ISO_8859_1),
                                StandardCharsets.UTF_8));
                        count++;
                        if (count == lines) {
                            break;
                        }
                    }
                }
                if (pos == 0) {
                    randomAccessFile.seek(0);
                    linesArray.add(new String(
                            randomAccessFile.readLine().getBytes(StandardCharsets.ISO_8859_1),
                            StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            throw new BioinfoException("读取日志失败");
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Collections.reverse(linesArray);
        linesArray.forEach(line -> result.append(line).append("\n"));
        return result.toString();
    }

    @Override
    public Task delBy(Integer id) {
        Task task = findById(id);
        try {
            lock.lock("task"+task.getId());
            taskRepository.delete(task);
        } finally {
            lock.unlock("task"+task.getId());
        }
        return task;
    }

    @Override
    public List<Task> listAll(CrudType crudEnum, Integer id) {
        return taskRepository.findAll(new Specification<Task>() {
            @Override
            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaQuery.where(criteriaBuilder.equal(root.get("crudEnum"),crudEnum),
                        criteriaBuilder.equal(root.get("objId"),id)).getRestriction();
            }
        });
    }

    //    @Override
//    public Task addTaskByCancerStudyId(Integer cancerStudyId){
//        CancerStudy cancerStudy = cancerStudyService.findCancerStudyById(cancerStudyId);
//        Task task = new Task();
////        task.setName(cancerStudy.getEnName());
//        task.setObjId(cancerStudy.getId());
//
//        int size = executor.getThreadPoolExecutor().getQueue().size();
//        if(size==QUEUE_CAPACITY) {
//            System.out.println("线程池队列已满" + size);
//            task.setTaskStatus(TaskStatus.UNTRACKING);
//        }else {
//            task.setTaskStatus(TaskStatus.PENDING);
//            asyncService.processAsyncByCancerStudy(task,cancerStudy);
//        }
//        return super.save(task);
//    }
//

//    @Override
//    public Task findByCodeAndObj(int codeId, int objId, TaskType taskType){
//        List<Task> tasks = taskRepository.findAll(new Specification<Task>() {
//            @Override
//            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
//                return criteriaQuery.where(criteriaBuilder.equal(root.get("codeId"),codeId),
//                        criteriaBuilder.equal(root.get("objId"),objId),
//                        criteriaBuilder.equal(root.get("taskType"),taskType)).getRestriction();
//            }
//        });
//        if(tasks.size()==0){
//            return null;
//        }
//        return tasks.get(0);
//    }

//    @Override
//    public Task addTask(Integer id, Integer cancerStudyId){
//        int size = executor.getThreadPoolExecutor().getQueue().size();
//        log.info("pool size {}",size);
//        CancerStudy cancerStudy = cancerStudyService.findCancerStudyById(cancerStudyId);
//        Code code = codeService.findById(id);
//        Task task = findByCodeAndObj(code.getId(), cancerStudy.getId(),TaskType.CANCER_STUDY);
//        if(task!=null && (task.getTaskStatus() == TaskStatus.PENDING || task.getTaskStatus()==TaskStatus.RUNNING)){
//            springWebSocketHandler.sendMessageToUsers(new TextMessage("Task 正在运行或者已经添加到队列！"));
//            throw new BioinfoException("Task 正在运行或者已经添加到队列！");
//        }
//        if(task==null){
//            task = new Task();
//            task.setTaskType(TaskType.CANCER_STUDY);
//        }
//        task.setObjId(cancerStudy.getId());
//        task.setCodeId(code.getId());
//        task.setTaskStatus(TaskStatus.PENDING);
//
//        if(size==QUEUE_CAPACITY) {
//            System.out.println("线程池队列已满" + size);
//            task.setTaskStatus(TaskStatus.UNTRACKING);
//            task = taskRepository.save(task);
//        }else {
//            task = taskRepository.save(task);
//            // 将task传给线程， 由对应的线程执行更新操作
//            codeService.runRCode(task, code, cancerStudy);
//        }
//        return task;
//    }
//
//    @Override
//    public Task runCancerStudyTask(int id){
//        Task task = findById(id);
//        return runCancerStudyTask(task);
//    }
//
//
//    public Task runCancerStudyTask(Task task){
//        CancerStudy cancerStudy = cancerStudyService.findCancerStudyById(task.getObjId());
//        Code code = codeService.findById(task.getCodeId());
//        codeService.runRCode(task, code, cancerStudy);
//        return task;
//    }

    @Override
    public List<Task> listPending(){
        return taskRepository.findAll(new Specification<Task>() {
            @Override
            public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaQuery.where(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("taskStatus"),TaskStatus.PENDING),
                        criteriaBuilder.equal(root.get("taskStatus"),TaskStatus.RUNNING)
                )).getRestriction();
            }
        });
    }

    @Override
    public  List<Task> addTas2Queue(){
        List<Task> tasks = listPending();
        if(tasks.size()>QUEUE_CAPACITY){
            tasks = tasks.subList(0,QUEUE_CAPACITY);
            List<Task> unDetail = tasks.subList(0,QUEUE_CAPACITY);
            System.out.println("未处理的Task:"+ unDetail.size());
        }
        if(tasks.size()!=0){
            tasks.forEach(task -> {
                log.info("服务器运行需要处理的task codeId：{}",task.getCodeId());
//                runCancerStudyTask(task);
//                CancerStudy cancerStudy = cancerStudyService.findCancerStudyById(task.getObjId());
//                codeService.processAsyncByCancerStudy(task,cancerStudy);
            });
        }
        return tasks;
    }

    private  void processMetadata(Task task, Table table,List<MetadataGroup> groups){
//            List<String[]> metadata = File2Tsv.tsvToList(uploadResult.getAbsolutePath());
//            String jsonString = JSON.toJSONString(metadata);
//            task.setGroups(jsonString);

        List<String> columnNames = table.columnNames();
        if(!columnNames.contains("Sample")){
            throw new BioinfoException("metadata 必须要包含列Sample!");
        }

        if(columnNames.contains("Group")){
            String groupListStr;
            if(groups!=null){
                groupListStr = JSONArray.toJSONString(groups);
            }else {

                String[] colors = new String[]{"orange", "green", "yellow", "purple", "red"};
                Column<String> group = (Column<String>) table.column("Group");
                List<String> groupList = group.asList().stream().distinct().collect(Collectors.toList());
//                List<MetadataGroup> metadataGroups = groupList.stream().map(item -> {
//
//                    return metadataGroup;
//                }).collect(Collectors.toList());
                List<MetadataGroup> metadataGroups = new ArrayList<>();
                for(int i =0;i<groupList.size();i++){
                    MetadataGroup metadataGroup = new MetadataGroup();
                    if(groupList.size()<colors.length){
                        metadataGroup.setColor(colors[i]);
                    }else {
                        metadataGroup.setColor("red");
                    }
                    metadataGroup.setName(groupList.get(i));
                    metadataGroups.add(metadataGroup);
                }
                groupListStr = JSONArray.toJSONString(metadataGroups);
//                groupListStr = JSONObject.toJSONString(groupList);
            }
            task.setGroups(groupListStr);
        }else {
            StringColumn column = StringColumn.create("Group");
            table = table.addColumns(column);
        }
        columnNames.remove("Group");
        columnNames.remove("Sample");
        columnNames.add(0,"Group");
        columnNames.add(0,"Sample");

        String columnNamesStr = JSONObject.toJSONString(columnNames);
        task.setMetadataColumnNames(columnNamesStr);

        int size = table.rowCount();
        table = table.where(Selection.withRange(0, size));
        String json = table.write().toString("json");
        task.setMetadataJson(json);
//            System.out.println();
//            try {
////                DataFrame<Object> dataFrame = DataFrame.readCsv(uploadResult.getAbsolutePath());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
    }


    @Override
    public Task upload(MultipartFile file, User user, Integer taskId, TaskParam taskParam) {
//        Optional<Task> taskOptional = taskRepository.findById(-1);
//        Task task;
//        if(!taskOptional.isPresent()){
//            task = new Task();
//        }else {
//            task = taskOptional.get();
//        }
        Task task = new Task();
        UploadResult uploadResult = fileHandlers.uploadFixed(file, "attachment", FileLocation.LOCAL);
        CsvReadOptions.Builder builder;

        if(taskParam.getFormat()!=null && "".equals(taskParam.getFormat())){
            if(taskParam.getFormat().equals("TSV")){
                builder=CsvReadOptions.builder(uploadResult.getAbsolutePath())
                        .separator('\t')										// table is tab-delimited
                        .header(true);
            }else if(taskParam.getFormat().equals("CSV")){
                builder=CsvReadOptions.builder(uploadResult.getAbsolutePath())
                        .separator(',')										// table is tab-delimited
                        .header(true);
            }else {
                throw new BioinfoException("您输入的文件不支持！");
            }
        }else {
            if(uploadResult.getAbsolutePath().endsWith(".tsv")){
                builder=CsvReadOptions.builder(uploadResult.getAbsolutePath())
                        .separator('\t')										// table is tab-delimited
                        .header(true);
            }else {
                builder=CsvReadOptions.builder(uploadResult.getAbsolutePath())
                        .separator(',')										// table is tab-delimited
                        .header(true);
            }
        }


        CsvReadOptions options = builder.build();
        Table table = Table.read().usingOptions(options);



        if(taskParam.getField().equals("metadata")){
            task.setMetadata(uploadResult.getAbsolutePath());
            processMetadata(task,table,null);
        }else if(taskParam.getField().equals("matrix")){
//            String[] list = File2Tsv.tsvToList(uploadResult.getAbsolutePath());
            task.setMatrix(uploadResult.getAbsolutePath());
//            Column<String > column = (Column<String>) table.column(0);
            List<String> columnList = table.columnNames();
            columnList.remove(0);
            StringColumn sample = StringColumn.create("Sample", columnList);
            StringColumn group= StringColumn.create("Group");
            Table metadata = Table.create(sample, group);
            String json = metadata.write().toString("json");
            task.setMetadataJson(json);
            List<String> columnNames = metadata.columnNames();
            String columnNamesStr = JSONObject.toJSONString(columnNames);
            task.setMetadataColumnNames(columnNamesStr);
        }

        return taskRepository.save(task);
    }

    @Override
    public boolean supportType(CrudType type) {
        return false;
    }
}
