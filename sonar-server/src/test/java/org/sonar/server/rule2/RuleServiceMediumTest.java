/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule2;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.DateUtils;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RuleServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .setProperty("sonar.es.http.port","9200");

  MyBatis myBatis = tester.get(MyBatis.class);
  RuleDao dao = tester.get(RuleDao.class);
  RuleIndex index = tester.get(RuleIndex.class);
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDataStores();
    dbSession = myBatis.openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_in_db_and_index_in_es() throws InterruptedException {
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    dao.insert(newRuleDto(ruleKey), dbSession);
    dbSession.commit();

    // verify that rule is persisted in db
    RuleDto persistedDto = dao.getByKey(ruleKey, dbSession);
    assertThat(persistedDto).isNotNull();
    assertThat(persistedDto.getId()).isGreaterThanOrEqualTo(0);
    assertThat(persistedDto.getRuleKey()).isEqualTo(ruleKey.rule());
    assertThat(persistedDto.getLanguage()).isEqualTo("js");
    assertThat(persistedDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(persistedDto.getSystemTags()).containsOnly("systag1", "systag2");

    // verify that rule is indexed in es
    index.refresh();

//    Thread.sleep(10000000);


    Rule hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.key().repository()).isEqualTo(ruleKey.repository());
    assertThat(hit.key().rule()).isEqualTo(ruleKey.rule());
    assertThat(hit.language()).isEqualTo("js");
    assertThat(hit.name()).isEqualTo("Rule S001");
    assertThat(hit.htmlDescription()).isEqualTo("Description S001");
    assertThat(hit.status()).isEqualTo(RuleStatus.READY);
    assertThat(hit.createdAt()).isNotNull();
    assertThat(hit.updatedAt()).isNotNull();
    assertThat(hit.internalKey()).isEqualTo("InternalKeyS001");
    assertThat(hit.severity()).isEqualTo("INFO");
    assertThat(hit.template()).isFalse();
    assertThat(hit.tags()).containsOnly("tag1", "tag2");
    assertThat(hit.systemTags()).containsOnly("systag1", "systag2");

  }

  @Test
  public void insert_and_index_rule_parameters() {
    // insert db
    RuleKey ruleKey = RuleKey.of("javascript", "S001");
    RuleDto ruleDto = newRuleDto(ruleKey);
    dao.insert(ruleDto, dbSession);
    dbSession.commit();

    RuleParamDto minParamDto = new RuleParamDto()
      .setName("min")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("2")
      .setDescription("Minimum");
    dao.addRuleParam(ruleDto, minParamDto, dbSession);
    RuleParamDto maxParamDto = new RuleParamDto()
      .setName("max")
      .setType(RuleParamType.INTEGER.type())
      .setDefaultValue("10")
      .setDescription("Maximum");
    dao.addRuleParam(ruleDto, maxParamDto, dbSession);
    dbSession.commit();

    // verify that parameters are persisted in db
    List<RuleParamDto> persistedDtos = dao.findRuleParamsByRuleKey(ruleKey, dbSession);
    assertThat(persistedDtos).hasSize(2);

    // verify that parameters are indexed in es
    index.refresh();
    Rule hit = index.getByKey(ruleKey);
    assertThat(hit).isNotNull();
    assertThat(hit.key()).isNotNull();


    RuleService service = tester.get(RuleService.class);
    Rule rule = service.getByKey(ruleKey);

    assertThat(rule.params()).hasSize(2);
    assertThat(Iterables.getLast(rule.params(), null).key()).isEqualTo("max");
  }

  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY.toString())
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setTags(ImmutableSet.of("tag1", "tag2"))
      .setSystemTags(ImmutableSet.of("systag1", "systag2"))
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix")
      .setCreatedAt(DateUtils.parseDate("2013-12-16"))
      .setUpdatedAt(DateUtils.parseDate("2013-12-17"));
  }
}
