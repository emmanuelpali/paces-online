package com.pacesonline.runservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RunServiceApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

  @Autowired
  private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
  void flywayCreatesRunsTable() {
      // Act: query PostgreSQL metadata.
      Integer tableCount = jdbcTemplate.queryForObject(
              """
              SELECT COUNT(*)
              FROM information_schema.tables
              WHERE table_schema = 'public'
                AND table_name = 'runs'
                AND table_type = 'BASE TABLE'
              """,
              Integer.class
      );

      assertThat(tableCount).isEqualTo(1);
  }

  @Test
  void healthEndpointReportsUp() throws Exception {
      mockMvc.perform(get("/actuator/health"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.status").value("UP"));
  }

}

