package net.causw.adapter.persistence.board;

import lombok.*;
import net.causw.adapter.persistence.circle.Circle;
import net.causw.adapter.persistence.post.Post;
import net.causw.adapter.persistence.base.BaseEntity;
import net.causw.domain.model.board.BoardDomainModel;
import net.causw.domain.model.enums.Role;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "tb_board")
public class Board extends BaseEntity {
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", nullable = true)
    private String description;

    @Column(name = "create_role_list", nullable = false)
    private String createRoles;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "is_deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted;

    @Column(name = "is_default", nullable = false)
    @ColumnDefault("false")
    private Boolean isDefault;

    @ManyToOne
    @JoinColumn(name = "circle_id", nullable = true)
    private Circle circle;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private Set<Post> postSet;


    private Board(
            String id,
            String name,
            String description,
            String createRoles,
            String category,
            Boolean isDeleted,
            Circle circle
    ) {
        super(id);
        this.name = name;
        this.description = description;
        this.createRoles = createRoles;
        this.category = category;
        this.isDeleted = isDeleted;
        this.circle = circle;
    }

    private Board(
            String name,
            String description,
            String createRoles,
            String category,
            Boolean isDeleted,
            Circle circle
    ) {
        this.name = name;
        this.description = description;
        this.createRoles = createRoles;
        this.category = category;
        this.isDeleted = isDeleted;
        this.circle = circle;
    }

    public static Board from(BoardDomainModel boardDomainModel) {
        Circle circle = boardDomainModel.getCircle().map(Circle::from).orElse(null);

        return new Board(
                boardDomainModel.getId(),
                boardDomainModel.getName(),
                boardDomainModel.getDescription(),
                String.join(",", boardDomainModel.getCreateRoleList()),
                boardDomainModel.getCategory(),
                boardDomainModel.getIsDeleted(),
                circle
        );
    }

    public static Board of(
            String name,
            String description,
            List<String> createRoleList,
            String category,
            Circle circle
    ) {
        if (createRoleList != null) {
            if (createRoleList.isEmpty()) {
                createRoleList.add(Role.ADMIN.getValue());
                createRoleList.add(Role.PRESIDENT.getValue());
            } else if (createRoleList.contains("ALL")) {
                createRoleList.addAll(
                        Arrays.stream(Role.values())
                                .map(Role::getValue)
                                .toList()
                );
                createRoleList.remove(Role.NONE.getValue());
                createRoleList.remove("ALL");
            } else {
                createRoleList = createRoleList
                        .stream()
                        .map(Role::of)
                        .map(Role::getValue)
                        .collect(Collectors.toList());
                createRoleList.add(Role.ADMIN.getValue());
                createRoleList.add(Role.PRESIDENT.getValue());
            }
        }
        return new Board(name, description, String.join(",", createRoleList), category, false,false, circle, new HashSet<>());
    }

    // 일반 게시판 생성
    public static Board fromName(String name){
        List<String> createRoleList = Arrays.stream(Role.values())
                .map(Role::getValue).collect(Collectors.toList());
        createRoleList.remove(Role.NONE.getValue());
        return new Board(name, "자유 게시판", String.join(",", createRoleList), "NORMAL", false, false, null, new HashSet<>());
    }

    public void setIsDeleted(boolean isDeleted){
        this.isDeleted = isDeleted;
    }

    public void update(String name, String description, String createRoles, String category){
        this.name = name;
        this.description = description;
        this.createRoles = createRoles;
        this.category = category;
    }
}
