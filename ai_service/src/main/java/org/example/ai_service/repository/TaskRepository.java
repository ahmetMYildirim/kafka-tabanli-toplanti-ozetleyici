package org.example.ai_service.repository;

import org.example.ai_service.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    Optional<TaskEntity> findByTaskId(String taskId);

    List<TaskEntity> findByMeetingId(Long meetingId);

    List<TaskEntity> findByAssignee(String assignee);

    List<TaskEntity> findByStatus(TaskEntity.Status status);

    List<TaskEntity> findByPriority(TaskEntity.Priority priority);

    List<TaskEntity> findByMeetingIdAndStatus(Long meetingId, TaskEntity.Status status);
}
