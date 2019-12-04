/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.test.bpmn.event.message;

import org.activiti.engine.impl.EventSubscriptionQueryImpl;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.Deployment;

/**

 */
public class MessageEventSubprocessTest extends PluggableActivitiTestCase {

  @Deployment
  public void testInterruptingUnderProcessDefinition() {
    testInterruptingUnderProcessDefinition(1, 3);
  }

  /**
   * Checks if unused event subscriptions are properly deleted.
   */
  @Deployment
  public void testTwoInterruptingUnderProcessDefinition() {
    testInterruptingUnderProcessDefinition(2, 4);
  }

  private void testInterruptingUnderProcessDefinition(int expectedNumberOfEventSubscriptions, int numberOfExecutions) {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // the process instance must have a message event subscription:
    Execution execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newMessage").singleResult();
    assertNotNull(execution);
    assertEquals(expectedNumberOfEventSubscriptions, createEventSubscriptionQuery().count());
    assertEquals(numberOfExecutions, runtimeService.createExecutionQuery().count());

    // if we trigger the usertask, the process terminates and the event subscription is removed:
    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("task", task.getTaskDefinitionKey());
    taskService.complete(task.getId());
    assertEquals(0, createEventSubscriptionQuery().count());
    assertEquals(0, runtimeService.createExecutionQuery().count());
    assertProcessEnded(processInstance.getId());

    // now we start a new instance but this time we trigger the event subprocess:
    processInstance = runtimeService.startProcessInstanceByKey("process");
    execution = runtimeService.createExecutionQuery().messageEventSubscriptionName("newMessage").singleResult();
    assertNotNull(execution);
    runtimeService.messageEventReceived("newMessage", execution.getId());

    task = taskService.createTaskQuery().singleResult();
    assertEquals("eventSubProcessTask", task.getTaskDefinitionKey());
    taskService.complete(task.getId());
    assertProcessEnded(processInstance.getId());
    assertEquals(0, createEventSubscriptionQuery().count());
    assertEquals(0, runtimeService.createExecutionQuery().count());
  }

  @Deployment
  public void testNonInterruptingUnderProcessDefinition() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // the process instance must have a message event subscription:
    Execution execution = runtimeService.createExecutionQuery()
                                        .processInstanceId(processInstance.getId())
                                        .messageEventSubscriptionName("newMessage")
                                        .singleResult();
    assertNotNull(execution);
    assertEquals(1, createEventSubscriptionQuery().count());
    assertEquals(3, runtimeService.createExecutionQuery().count());

    // if we trigger the usertask, the process terminates and the event
    // subscription is removed:
    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("task", task.getTaskDefinitionKey());
    taskService.complete(task.getId());
    assertEquals(0, createEventSubscriptionQuery().count());
    assertEquals(0, runtimeService.createExecutionQuery().count());

    // ###################### now we start a new instance but this time we
    // trigger the event subprocess:
    processInstance = runtimeService.startProcessInstanceByKey("process");
    
    execution = runtimeService.createExecutionQuery()
                              .processInstanceId(processInstance.getId())
                              .messageEventSubscriptionName("newMessage")
                              .singleResult();
    
    runtimeService.messageEventReceived("newMessage", execution.getId());

    assertEquals(2, taskService.createTaskQuery().count());

    // now let's first complete the task in the main flow:
    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // we still have 3 executions:
    assertEquals(3, runtimeService.createExecutionQuery().count());

    // now let's complete the task in the event subprocess
    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // done!
    assertEquals(0, runtimeService.createExecutionQuery().count());

    // #################### again, the other way around:

    processInstance = runtimeService.startProcessInstanceByKey("process");
    execution = runtimeService.createExecutionQuery()
                              .processInstanceId(processInstance.getId())
                              .messageEventSubscriptionName("newMessage")
                              .singleResult();
    
    runtimeService.messageEventReceived("newMessage", execution.getId());

    assertEquals(2, taskService.createTaskQuery().count());

    task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
    taskService.complete(task.getId());
    // we still have task executions:
    assertEquals(2, runtimeService.createExecutionQuery().count());

    task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
    taskService.complete(task.getId());
    // done!
    assertEquals(0, runtimeService.createExecutionQuery().count());
    
  }

  private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
    return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor());
  }

}
