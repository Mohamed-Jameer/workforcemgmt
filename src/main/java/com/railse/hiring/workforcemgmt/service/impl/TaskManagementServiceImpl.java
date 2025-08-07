package com.railse.hiring.workforcemgmt.service.impl;



import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.*;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {
    private final TaskRepository taskRepository;
    private final ITaskManagementMapper taskMapper;

    public TaskManagementServiceImpl(TaskRepository taskRepository, ITaskManagementMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskMapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();
        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");
            newTask.getActivityHistory().add(new TaskActivity("Created", System.currentTimeMillis()));
            createdTasks.add(taskRepository.save(newTask));
        }
        return taskMapper.modelListToDtoList(createdTasks);
    }

    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();
        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));

            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
                task.getActivityHistory().add(new TaskActivity("Status changed to " + item.getTaskStatus(), System.currentTimeMillis()));
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
            }
            updatedTasks.add(taskRepository.save(task));
        }
        return taskMapper.modelListToDtoList(updatedTasks);
    }

    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());
            // CANCEL all tasks for this ref/type
            for (TaskManagement taskToCancel : tasksOfType) {
                taskToCancel.setStatus(TaskStatus.CANCELLED);
                taskToCancel.getActivityHistory().add(new TaskActivity("Cancelled by reassignment", System.currentTimeMillis()));
                taskRepository.save(taskToCancel);
            }
            // create new task for the new assignee
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(request.getReferenceId());
            newTask.setReferenceType(request.getReferenceType());
            newTask.setTask(taskType);
            newTask.setAssigneeId(request.getAssigneeId());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("Task reassigned by assign-by-ref");
            newTask.getActivityHistory().add(new TaskActivity("Assigned to user " + request.getAssigneeId(), System.currentTimeMillis()));
            taskRepository.save(newTask);
        }
        return "Tasks reassigned and old assignments cancelled for reference " + request.getReferenceId();
    }


@Override
public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
    List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());

    System.out.println("Total tasks before filtering: " + tasks.size());

    List<TaskManagement> filteredTasks = tasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
            .filter(task -> {
                boolean inRange = task.getTaskDeadlineTime() >= request.getStartDate()
                        && task.getTaskDeadlineTime() <= request.getEndDate();
                boolean activeBeforeStart = task.getTaskDeadlineTime() < request.getStartDate()
                        && task.getStatus() != TaskStatus.COMPLETED;
                return inRange || activeBeforeStart;
            })
            .collect(Collectors.toList());

    System.out.println("Total tasks after filtering: " + filteredTasks.size());

    return taskMapper.modelListToDtoList(filteredTasks);
}


    @Override
    public void updatePriority(Long taskId, Priority priority) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        task.setPriority(priority);
        task.getActivityHistory().add(new TaskActivity("Priority changed to " + priority, System.currentTimeMillis()));
        taskRepository.save(task);
    }

    @Override
    public List<TaskManagementDto> getTasksByPriority(Priority priority) {
        return taskMapper.modelListToDtoList(taskRepository.findByPriority(priority));
    }

    @Override
    public void addComment(Long taskId, TaskCommentRequest request) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        TaskComment comment = new TaskComment();
        comment.setUserId(request.getUserId());
        comment.setComment(request.getComment());
        comment.setTimestamp(System.currentTimeMillis());
        task.getComments().add(comment);
        task.getActivityHistory().add(new TaskActivity("Comment added by user " + request.getUserId(), System.currentTimeMillis()));
        taskRepository.save(task);
    }
}