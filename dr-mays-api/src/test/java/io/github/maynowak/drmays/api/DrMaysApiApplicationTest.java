package io.github.maynowak.drmays.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class DrMaysApiApplicationTest {

  @Autowired private ApplicationContext context;

  @Test
  void contextLoadsWithApplicationBean() {
    assertThat(context.getBean(DrMaysApiApplication.class)).isNotNull();
  }
}
