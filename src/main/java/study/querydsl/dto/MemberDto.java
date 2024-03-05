package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection // 어노테이션 선언 후, gradle의 clean => build를 해주면 generated 폴더에 dto 생성
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
