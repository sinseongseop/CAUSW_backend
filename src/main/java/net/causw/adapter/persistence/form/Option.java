package net.causw.adapter.persistence.form;

import jakarta.persistence.*;
import lombok.*;
import net.causw.adapter.persistence.base.BaseEntity;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "tb_option")
public class Option extends BaseEntity {
    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    public static Option of(Integer number, String text, Question question) {
        return new Option(number, text, false, question);
    }

    public void setIsSelected(Boolean isSelected) {
        this.isSelected = isSelected;
    }
}