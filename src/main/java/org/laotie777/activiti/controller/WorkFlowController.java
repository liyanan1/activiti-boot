package org.laotie777.activiti.controller;

import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.laotie777.activiti.entity.LeaveBill;
import org.laotie777.activiti.entity.WorkflowBean;
import org.laotie777.activiti.service.ILeaveBillService;
import org.laotie777.activiti.service.IWorkflowService;
import org.laotie777.activiti.service.impl.WorkflowServiceImpl;
import org.laotie777.activiti.utils.SpringWebUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author yuh
 * 2018/1/15.
 */
@RestController
@RequestMapping("/workflow")
public class WorkFlowController {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkFlowController.class);

    @Autowired
    private IWorkflowService workflowService;

    @Autowired
    private ILeaveBillService leaveBillService;


    /**
     * 查询流程定义列表 和 流程部署列表
     *
     * @return
     */
    @RequestMapping("deployHome")
    public ModelAndView deployHome() {

        List<Deployment> deploymentList = workflowService.findDeploymentList();
        List<ProcessDefinition> processDefinitionList = workflowService.findProcessDefinitionList();

        ModelAndView mav = new ModelAndView();
        mav.addObject("depList", deploymentList);
        mav.addObject("pdList", processDefinitionList);
        mav.setViewName("/workflow/workflow");
        return mav;
    }


    /**
     * 流程部署
     *
     * @param bean
     * @return
     */
    @RequestMapping("newdeploy")
    public ModelAndView newdeploy(String fileName, @RequestParam("file") MultipartFile file) {

        ModelAndView mav = new ModelAndView();
        mav.setViewName("forward:/workflow/deployHome");

        try {
            File jdkFile = new File("./1.zip");
            org.apache.commons.io.FileUtils.copyInputStreamToFile(file.getInputStream(), jdkFile);
            workflowService.saveNewDeploye(jdkFile, fileName);
        } catch (Exception e) {
            logger.error(fileName + "=> 文件上传失败");
        }
        return mav;

    }

    /**
     * 流程部署
     *
     * @param bean
     * @return
     */
    @RequestMapping("newdeployFile")
    public ModelAndView newdeploy(@RequestParam("png") MultipartFile png, @RequestParam("bpmn") MultipartFile bpmn,String fileName) {

        ModelAndView mav = new ModelAndView();
        mav.setViewName("forward:/workflow/deployHome");

        try {
            workflowService.saveNewDeploye(png, bpmn,fileName);
        } catch (Exception e) {
            logger.error("文件上传失败");
        }
        return mav;

    }


    /**
     * 查看流程图
     * @param id
     * @param diagramResourceName
     * @param response
     */
    @RequestMapping("viewImage/{id}/{diagramResourceName}")
    public void viewImage(@PathVariable(name = "id") String deploymentId,
                          @PathVariable(name = "diagramResourceName") String diagramResourceName,
                          HttpServletResponse response) {

        ServletOutputStream outputStream = null;
        try {
            InputStream imageInputStream = workflowService.findImageInputStream(deploymentId, diagramResourceName+".png");
            byte[] arr = new byte[1024];
            int f = -1;
            outputStream = response.getOutputStream();
            while ((f = imageInputStream.read(arr)) != -1) {
                outputStream.write(arr, 0, f);
            }
            outputStream.flush();
        } catch (IOException e) {
            logger.error("读取流程图失败");
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {

            }
        }
    }



    /**
     * 流程删除
     *
     * @param bean
     * @return
     */
    @RequestMapping("delDeployment/{id}")
    public ModelAndView delDeployment(@PathVariable(name = "id") String deploymentId){

        ModelAndView mav = new ModelAndView();
        mav.setViewName("forward:/workflow/deployHome");

        try {
           workflowService.deleteProcessDefinitionByDeploymentId(deploymentId);
        } catch (Exception e) {
            logger.error(deploymentId + "=> 流程删除失败");
        }
        return mav;

    }


    /**
     * 启动流程实例
     * @param id
     * @return
     */
    @RequestMapping("startProcess/{id}")
    public ModelAndView startProcess(@PathVariable(name = "id") String id){

        ModelAndView mav = new ModelAndView();
        mav.setViewName("redirect:/leaveBill/home");

        try {
            workflowService.saveStartProcess(Long.parseLong(id));
        } catch (Exception e) {
            logger.error(id + "=> 流程启动失败");
        }
        return mav;

    }


    /**
     * 查询个人任务
     *
     * @param bean
     * @return
     */
    @RequestMapping("listTask")
    public ModelAndView listTask(){

        ModelAndView mav = new ModelAndView();
        mav.setViewName("/workflow/task");

        try {
            List<Task> taskListByName = workflowService.findTaskListByName(SpringWebUtil.get().getName());
            mav.addObject("list",taskListByName);
        } catch (Exception e) {
            logger.error("任务查询失败");
        }
        return mav;

    }


    /**
     * 根据formKey获得下一步的去向
     * @param taskId
     * @return
     */
    @RequestMapping("viewTaskForm/{id}")
    public ModelAndView viewTaskForm(@PathVariable(name = "id")String taskId){
        //去他该去的地方
        String taskFormKeyByTaskId = workflowService.findTaskFormKeyByTaskId(taskId);
        ModelAndView mav = new ModelAndView();
        mav.setViewName("redirect:"+taskFormKeyByTaskId+"/"+taskId);
        return mav;

    }


    /**
     * 带着请假单去审核
     * @param taskId
     * @return
     */
    @RequestMapping("audit/{id}")
    public ModelAndView audit(@PathVariable(name = "id")String taskId){
        LeaveBill leaveBillByTaskId = workflowService.findLeaveBillByTaskId(taskId);
        //查到请假单
        ModelAndView mav = new ModelAndView();

        //请假单
        mav.addObject("leaveBill",leaveBillByTaskId);
        //任务ID
        mav.addObject("taskId",taskId);

        //批注
        List<Comment> commentByTaskId = workflowService.findCommentByTaskId(taskId);
        mav.addObject("comments",commentByTaskId);

        //连线
        List<String> outComeListByTaskId = workflowService.findOutComeListByTaskId(taskId);
        mav.addObject("links",outComeListByTaskId);

        //到审核页面
        mav.setViewName("/workflow/taskForm");
        return mav;

    }


    /**
     * 完成任务
     * @param bean
     * @return
     */
    @RequestMapping("submitTask")
    public ModelAndView submitTask(WorkflowBean bean){
        workflowService.saveSubmitTask(bean);
        ModelAndView mav = new ModelAndView();
        mav.setViewName("redirect:/workflow/listTask");
        return mav;

    }

    /**
     * 查询历史请假信息
     * @param id
     * @return
     */
    @RequestMapping("viewHisComment/{id}")
    public ModelAndView viewHisComment(@PathVariable(name = "id")String id){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("/workflow/taskFormHis");
        LeaveBill leaveBillById = leaveBillService.findLeaveBillById(Long.parseLong(id));
        mav.addObject("leaveBill",leaveBillById);
        List<Comment> commentByLeaveBillId = workflowService.findCommentByLeaveBillId(Long.parseLong(id));
        mav.addObject("comments",commentByLeaveBillId);
        return mav;

    }

    /**
     * 查看当前流程图
     * @param id
     * @return
     */
    @RequestMapping("viewCurrentImage/{id}")
    public ModelAndView viewCurrentImage(@PathVariable(name = "id")String id){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("/workflow/image");
        ProcessDefinition processDefinitionByTaskId = workflowService.findProcessDefinitionByTaskId(id);
        mav.addObject("deploymentId",processDefinitionByTaskId.getDeploymentId());
        mav.addObject("imageName",processDefinitionByTaskId.getDiagramResourceName());
        Map<String, Object> coordingByTask = workflowService.findCoordingByTask(id);
        mav.addObject("acs",coordingByTask);
        return mav;

    }







}

