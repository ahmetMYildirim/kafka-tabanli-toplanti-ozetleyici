package repository;

import entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    
    List<TaskEntity> findByMeetingId(Long meetingId);
    
    Optional<TaskEntity> findByTaskId(String taskId);
    
    List<TaskEntity> findByStatus(TaskEntity.Status status);
    
    List<TaskEntity> findByPriority(TaskEntity.Priority priority);
}

