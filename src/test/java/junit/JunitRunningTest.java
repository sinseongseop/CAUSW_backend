package junit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.within;

@ExtendWith(MockitoExtension.class)
public class JunitRunningTest {

    @Test
    @DisplayName("AssertJ 동작 확인")
    void assertjRunningTest(){
        String test = "AssertJ running test";
        assertThat(test).isEqualTo("AssertJ running test");
    }

    @Test
    @DisplayName("예외 catch 관련 테스트")
    void catchExceptionTest() {
        Throwable thrown = catchThrowable(() -> {
            throw new IllegalArgumentException("Invalid argument");
        });

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid argument");
    }

    @Test
    @DisplayName("String 관련 테스트")
    void stringAssertionsTest() {
        String testString = "CAU is great!";

        assertThat(testString)
                .startsWith("CAU")
                .endsWith("great!")
                .contains("is")
                .doesNotContain("bad")
                .hasSize(13);
    }

    @Test
    @DisplayName("List 관련 테스트")
    void listAssertionsTest() {
        List<String> testList = Arrays.asList("AssertJ", "Mockito", "JUnit");

        assertThat(testList)
                .isNotEmpty()
                .hasSize(3)
                .contains("Mockito")
                .doesNotContain("Hamcrest")
                .containsExactly("AssertJ", "Mockito", "JUnit")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Map 관련 테스트")
    void mapAssertionsTest() {
        Map<String, Integer> testMap = new HashMap<>();
        testMap.put("one", 1);
        testMap.put("two", 2);
        testMap.put("three", 3);

        assertThat(testMap)
                .isNotEmpty()
                .hasSize(3)
                .containsKeys("one", "two")
                .containsValues(1, 2)
                .doesNotContainKeys("four")
                .doesNotContainValue(4);
    }

    @Test
    @DisplayName("객체 속성 테스트")
    void objectPropertyTest() {
        Person person = new Person("John", 25);

        assertThat(person)
                .isNotNull()
                .extracting("name", "age")
                .containsExactly("John", 25);

        assertThat(person)
                .hasFieldOrPropertyWithValue("name", "John")
                .hasFieldOrPropertyWithValue("age", 25);

    }

    class Person {
        private String name;
        private int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    @Test
    @DisplayName("조건 검증 테스트")
    void conditionalAssertionsTest() {
        int value = 10;

        assertThat(value)
                .isGreaterThan(5)
                .isLessThanOrEqualTo(10)
                .isBetween(1, 20);
    }

    @Test
    @DisplayName("Null 검증 테스트")
    void nullAssertionsTest() {
        Object nullObject = null;
        Object nonNullObject = new Object();

        assertThat(nullObject).isNull();
        assertThat(nonNullObject).isNotNull();
    }

    @Test
    @DisplayName("부동 소수점 비교 테스트")
    void floatingPointAssertionsTest() {
        double number = 1.0 / 3.0;

        assertThat(number).isCloseTo(0.333, within(0.001));
    }

}
