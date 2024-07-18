package net.causw.application;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class JunitRunningTest {

    @Test
    @DisplayName("AssertJ 동작 확인")
    void assertjRunningTest(){
        String test = "AssertJ running test";
        assertThat(test).isEqualTo("AssertJ running test");
    }

}
