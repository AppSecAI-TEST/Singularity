package com.hubspot.singularity.mesos;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public class SingularityOfferHolder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityOfferHolder.class);

  private final List<Protos.Offer> offers;
  private final List<SingularityTask> acceptedTasks;
  private final Set<SingularityPendingTaskId> rejectedPendingTaskIds;
  private List<Resource> currentResources;
  private Set<String> roles;

  private final String rackId;
  private final String slaveId;
  private final String hostname;
  private final String sanitizedHost;
  private final String sanitizedRackId;

  private final Map<String, String> textAttributes;
  private final Map<String, String> reservedSlaveAttributes;

  public SingularityOfferHolder(List<Protos.Offer> offers, int taskSizeHint, String rackId, String slaveId, String hostname, Map<String, String> textAttributes, Map<String, String> reservedSlaveAttributes) {
    this.rackId = rackId;
    this.slaveId = slaveId;
    this.hostname = hostname;
    this.offers = offers;
    this.roles = MesosUtils.getRoles(offers.get(0));
    this.acceptedTasks = Lists.newArrayListWithExpectedSize(taskSizeHint);
    this.currentResources = offers.size()  > 1 ? MesosUtils.combineResources(offers.stream().map(Protos.Offer::getResourcesList).collect(Collectors.toList())) : offers.get(0).getResourcesList();
    this.rejectedPendingTaskIds = new HashSet<>();
    this.sanitizedHost = JavaUtils.getReplaceHyphensWithUnderscores(hostname);
    this.sanitizedRackId = JavaUtils.getReplaceHyphensWithUnderscores(rackId);
    this.textAttributes = textAttributes;
    this.reservedSlaveAttributes = reservedSlaveAttributes;
  }

  public Map<String, String> getTextAttributes() {
    return textAttributes;
  }

  public String getRackId() {
    return rackId;
  }

  public String getSlaveId() {
    return slaveId;
  }

  public boolean hasReservedSlaveAttributes() {
    return !reservedSlaveAttributes.isEmpty();
  }

  public Map<String, String> getReservedSlaveAttributes() {
    return reservedSlaveAttributes;
  }

  public String getHostname() {
    return hostname;
  }

  public String getSanitizedHost() {
    return sanitizedHost;
  }

  public String getSanitizedRackId() {
    return sanitizedRackId;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void addRejectedTask(SingularityPendingTaskId pendingTaskId) {
    rejectedPendingTaskIds.add(pendingTaskId);
  }

  public boolean hasRejectedPendingTaskAlready(SingularityPendingTaskId pendingTaskId) {
    return rejectedPendingTaskIds.contains(pendingTaskId);
  }

  public void addMatchedTask(SingularityTask task) {
    LOG.trace("Accepting task {} for offers {}", task.getTaskId(), offers.stream().map(Offer::getId).collect(Collectors.toList()));
    acceptedTasks.add(task);

    // subtract task resources from offer
    currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getResourcesList());

    // subtract executor resources from offer, if any are defined
    if (task.getMesosTask().hasExecutor() && task.getMesosTask().getExecutor().getResourcesCount() > 0) {
      currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getExecutor().getResourcesList());
    }
  }

  public void launchTasks(SchedulerDriver driver) {
    final List<TaskInfo> toLaunch = Lists.newArrayListWithCapacity(acceptedTasks.size());
    final List<SingularityTaskId> taskIds = Lists.newArrayListWithCapacity(acceptedTasks.size());

    for (SingularityTask task : acceptedTasks) {
      taskIds.add(task.getTaskId());
      toLaunch.add(task.getMesosTask());
      LOG.debug("Launching {} with offer {}", task.getTaskId(), offers.get(0).getId());
      LOG.trace("Launching {} mesos task: {}", task.getTaskId(), MesosUtils.formatForLogging(task.getMesosTask()));
    }

    Status initialStatus = driver.launchTasks(offers.stream().map(Protos.Offer::getId).collect(Collectors.toList()), toLaunch);

    LOG.info("{} tasks ({}) launched with status {}", taskIds.size(), taskIds, initialStatus);
  }

  public List<SingularityTask> getAcceptedTasks() {
    return acceptedTasks;
  }

  public List<Resource> getCurrentResources() {
    return currentResources;
  }

  public List<Protos.Offer> getOffers() {
    return offers;
  }

  @Override
  public String toString() {
    return "SingularityOfferHolder{" +
        "offers=" + offers +
        ", acceptedTasks=" + acceptedTasks +
        ", rejectedPendingTaskIds=" + rejectedPendingTaskIds +
        ", currentResources=" + currentResources +
        ", roles=" + roles +
        ", rackId='" + rackId + '\'' +
        ", slaveId='" + slaveId + '\'' +
        ", hostname='" + hostname + '\'' +
        ", sanitizedHost='" + sanitizedHost + '\'' +
        ", sanitizedRackId='" + sanitizedRackId + '\'' +
        ", textAttributes=" + textAttributes +
        ", reservedSlaveAttributes=" + reservedSlaveAttributes +
        '}';
  }
}
