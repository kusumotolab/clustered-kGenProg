package jp.kusumotolab.kgenprog.coordinator;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class RequestValidatorTest {

  @Test
  public void testValidate() throws InterruptedException {
    final RequestValidator validator = new RequestValidator();
    final ExecuteTestRequest executeTestRequest1 = new ExecuteTestRequest(null, null, "localhost",
        8080, 0, 0);
    final ExecuteTestRequest executeTestRequest2 = new ExecuteTestRequest(null, null, "localhost",
        8080, 0, 0);
    final ExecuteTestRequest executeTestRequest3 = new ExecuteTestRequest(null, null, "localhost",
        8080, 0, 0);

    assertThat(validator.validate(executeTestRequest1)).isTrue();

    validator.addInvalidateRequest(executeTestRequest2);
    assertThat(validator.validate(executeTestRequest1)).isFalse();
    assertThat(validator.validate(executeTestRequest3)).isFalse();

    Thread.sleep(1);
    final ExecuteTestRequest executeTestRequest4 = new ExecuteTestRequest(null, null, "localhost",
        8080, 0, 0);
    assertThat(validator.validate(executeTestRequest4)).isTrue();
  }
}