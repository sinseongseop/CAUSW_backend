package net.causw.adapter.persistence.repository.form;

import net.causw.adapter.persistence.form.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyRepository  extends JpaRepository<Reply, String> {
    Optional<Reply> findById(String id);

    List<Reply> findAllByFormId(String formId);
}