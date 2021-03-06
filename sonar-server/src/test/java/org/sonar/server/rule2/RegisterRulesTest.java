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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.rule.RuleTagDto;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.server.qualityprofile.ProfilesManager;
import org.sonar.server.rule.ESRuleTags;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.rule.RuleRepositories;

import java.util.Collection;
import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterRulesTest extends AbstractDaoTestCase {

  private static final String[] EXCLUDED_COLUMN_NAMES = {"created_at", "updated_at", "note_data", "note_user_login", "note_created_at", "note_updated_at"};
  private static final String[] EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT = {"created_at", "updated_at", "note_data", "note_user_login", "note_created_at", "note_updated_at",
    "characteristic_id", "default_characteristic_id",
    "remediation_function", "default_remediation_function", "remediation_coeff", "default_remediation_coeff", "remediation_offset", "default_remediation_offset",
    "effort_to_fix_description"};

  RegisterRules task;

  @Mock
  ProfilesManager profilesManager;

  @Mock
  RuleRegistry ruleRegistry;

  @Mock
  ESRuleTags esRuleTags;

  @Captor
  ArgumentCaptor<Collection<RuleDto>> rulesCaptor;

  @Captor
  ArgumentCaptor<Collection<RuleTagDto>> ruleTagsCaptor;

  MyBatis myBatis;
  RuleDao ruleDao;
  RuleTagDao ruleTagDao;
  ActiveRuleDao activeRuleDao;
  CharacteristicDao characteristicDao;
  System2 system;
  WorkQueue queue;
  Date date = DateUtils.parseDateTime("2014-03-17T19:10:03+0100");

  private DbSession session;

  @Before
  public void before() {
    system = mock(System2.class);
    when(system.now()).thenReturn(date.getTime());
    myBatis = getMyBatis();
    ruleDao = new RuleDao();
    ruleTagDao = new RuleTagDao(myBatis);
    activeRuleDao = new ActiveRuleDao(new QualityProfileDao(myBatis), ruleDao);
    characteristicDao = new CharacteristicDao(myBatis);
    task = new RegisterRules(new RuleDefinitionsLoader(mock(RuleRepositories.class), new RulesDefinition[]{new FakeRepository()}),
      profilesManager, myBatis, ruleDao, activeRuleDao, characteristicDao, system);
    session = myBatis.openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void update_template_rule_language() {
    setupData("update_template_rule_language");
    task.start();

    checkTables("update_template_rule_language", EXCLUDED_COLUMN_NAMES, "rules");
  }

  /**
   * SONAR-4642
   */
  @Test
  public void notify_for_removed_rules_when_repository_is_still_existing() {
    setupData("notify_for_removed_rules_when_repository_is_still_existing");
    task.start();

    verify(profilesManager).removeActivatedRules(1);
  }

  /**
   * SONAR-4642
   */
  @Test
  public void not_notify_for_removed_rules_when_repository_do_not_exists_anymore() {
    setupData("shared");
    task.start();

    verifyZeroInteractions(profilesManager);
  }

  @Test
  @Ignore //TODO Check if created and updated should be at DTO/BaseDao level.
  public void reactivate_disabled_rules() {
    setupData("reactivate_disabled_rules");
    task.start();

    checkTables("reactivate_disabled_rules", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules");

    assertThat(ruleDao.getById(1, session).getUpdatedAt()).isNotNull();
  }

  @Test
  public void reactivate_disabled_template_rules() {
    setupData("reactivate_disabled_template_rules");
    task.start();

    checkTables("reactivate_disabled_template_rules", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules");
  }

  @Test
  public void disable_deprecated_active_rules() {
    setupData("disable_deprecated_active_rules");
    task.start();

    checkTables("disable_deprecated_active_rules", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules");
  }

  @Test
  @Ignore //TODO to fix with new RegisterRule
  public void disable_deprecated_active_rule_params() {
    setupData("disable_deprecated_active_rule_params");
    task.start();

    checkTables("disable_deprecated_active_rule_params", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules", "rules_parameters", "active_rules", "active_rule_parameters");
  }

  @Test
  //TODO check with mergeTag what happens on removal.
  public void disable_deprecated_rules() {
    setupData("disable_deprecated_rules");
    task.start();

    checkTables("disable_deprecated_rules", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules", "rules_parameters");
  }

  @Test
  @Ignore //TODO to fix with new RegisterRule
  public void not_disable_already_disabled_rules() {
    setupData("not_disable_already_disabled_rules");
    task.start();

    checkTables("not_disable_already_disabled_rules", new String[]{"created_at", "note_data", "note_user_login", "note_created_at", "note_updated_at"}, "rules");
  }

  @Test
  @Ignore //TODO fix in RuleRegister DAOv.2
  public void update_rule_fields() {
    setupData("update_rule_fields");
    task.start();

    checkTables("update_rule_fields", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters");
  }

  @Test
  public void update_rule_parameters() {
    setupData("update_rule_parameters");
    task.start();

    checkTables("update_rule_parameters", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules", "rules_parameters");
  }

  @Test
  @Ignore //TODO characteristics not yet in DAOv.2
  public void set_no_default_characteristic_when_characteristic_not_found() {
    setupData("set_no_characteristic_when_characteristic_not_found");

    task.start();
    // Warning log should be displayed

    checkTables("set_no_characteristic_when_characteristic_not_found", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void set_no_default_characteristic_when_default_characteristic_not_found_and_overriding_characteristic_disabled() {
    setupData("set_no_characteristic_when_default_characteristic_not_found_and_overriding_characteristic_disabled");

    task.start();
    // No log should be displayed

    checkTables("set_no_characteristic_when_default_characteristic_not_found_and_overriding_characteristic_disabled", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  @Ignore //TODO characteristics not yet in DAOv.2
  public void set_no_default_characteristic_when_default_characteristic_not_found_but_characteristic_has_been_overridden() {
    setupData("set_no_default_characteristic_when_default_characteristic_not_found_but_characteristic_has_been_overridden");

    task.start();
    // No log should be displayed

    checkTables("set_no_default_characteristic_when_default_characteristic_not_found_but_characteristic_has_been_overridden", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  @Ignore //TODO to fix with new RegisterRule
  public void fail_when_rule_is_linked_on_root_characteristic() {
    setupData("ignore_rule_debt_definitions_if_rule_is_linked_on_root_characteristic");

    try {
      task.start();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class).hasMessage("Rule 'fake:rule1' cannot be linked on the root characteristic 'MEMORY_EFFICIENCY'");
    }
  }

  @Test
  public void not_disable_template_rules_if_parent_is_enabled() {
    setupData("not_disable_template_rules_if_parent_is_enabled");
    task.start();

    checkTables("not_disable_template_rules_if_parent_is_enabled", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules");
  }

  @Test
  @Ignore //TODO to fix with new RegisterRule
  public void disable_template_rules_if_parent_is_disabled() {
    setupData("disable_template_rules_if_parent_is_disabled");
    task.start();

    checkTables("disable_template_rules_if_parent_is_disabled", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules");
  }

  @Test
  public void not_disable_manual_rules() {
    // the hardcoded repository "manual" is used for manual violations
    setupData("not_disable_manual_rules");
    task.start();

    checkTables("not_disable_manual_rules", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules");
  }

  @Test
  public void test_high_number_of_rules() {
    task = new RegisterRules(new RuleDefinitionsLoader(mock(RuleRepositories.class), new RulesDefinition[]{new BigRepository()}),
      profilesManager, myBatis, ruleDao, activeRuleDao, characteristicDao);

    setupData("shared");
    task.start();


    // There is already one rule in DB
    assertThat(ruleDao.findAll(session)).hasSize(BigRepository.SIZE + 1);
    assertThat(ruleDao.findAllRuleParams(session)).hasSize(BigRepository.SIZE * 20);
//    assertThat(ruleDao.selectTags(getMyBatis().openSession(false))).hasSize(BigRepository.SIZE * 3);
  }

  @Test
  public void insert_extended_repositories() {
    task = new RegisterRules(new RuleDefinitionsLoader(mock(RuleRepositories.class), new RulesDefinition[]{
      new FindbugsRepository(), new FbContribRepository()}),
      profilesManager, myBatis, ruleDao, activeRuleDao, characteristicDao
    );


    setupData("empty");
    task.start();

    checkTables("insert_extended_repositories", EXCLUDED_COLUMN_NAMES_INCLUDING_DEBT, "rules");
  }

  static class FakeRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("fake", "java");

      NewRule rule1 = repo.createRule("rule1")
        .setName("One")
        .setHtmlDescription("Description of One")
        .setSeverity(Severity.BLOCKER)
        .setInternalKey("config1")
        .setTags("tag1", "tag3", "tag5");

      rule1.setDebtSubCharacteristic("MEMORY_EFFICIENCY")
        .setDebtRemediationFunction(rule1.debtRemediationFunctions().linearWithOffset("5d", "10h"))
        .setEffortToFixDescription("squid.S115.effortToFix");

      rule1.createParam("param1").setDescription("parameter one").setDefaultValue("default value one");
      rule1.createParam("param2").setDescription("parameter two").setDefaultValue("default value two");

      repo.createRule("rule2")
        .setName("Two")
        .setHtmlDescription("Description of Two")
        .setSeverity(Severity.INFO)
        .setStatus(RuleStatus.DEPRECATED);
      repo.done();
    }
  }

  static class BigRepository implements RulesDefinition {
    static final int SIZE = 500;

    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("big", "java");
      for (int i = 0; i < SIZE; i++) {
        NewRule rule = repo.createRule("rule" + i)
          .setName("name of " + i)
          .setHtmlDescription("description of " + i)
          .setSeverity(Severity.BLOCKER)
          .setInternalKey("config1")
          .setTags("tag1", "tag3", "tag5");
        for (int j = 0; j < 20; j++) {
          rule.createParam("param" + j);
        }

      }
      repo.done();
    }
  }

  static class FindbugsRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("findbugs", "java");
      repo.createRule("rule1")
        .setName("Rule One")
        .setHtmlDescription("Description of Rule One");
      repo.done();
    }
  }

  static class FbContribRepository implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewExtendedRepository repo = context.extendRepository("findbugs", "java");
      repo.createRule("rule2")
        .setName("Rule Two")
        .setHtmlDescription("Description of Rule Two");
      repo.done();
    }
  }
}

