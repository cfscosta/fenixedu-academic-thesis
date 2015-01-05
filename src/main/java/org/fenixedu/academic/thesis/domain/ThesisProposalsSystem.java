package org.fenixedu.academic.thesis.domain;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeHelper;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.bennu.core.groups.Group;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class ThesisProposalsSystem extends ThesisProposalsSystem_Base {

    private ThesisProposalsSystem() {
	super();
	setBennu(Bennu.getInstance());
	setCanManageGroup(DynamicGroup.get("managers"));
	setCanCreateThesisGroup(DynamicGroup.get("thesisCreators"));
    }

    public static ThesisProposalsSystem getInstance() {
	if (Bennu.getInstance().getThesisProposalsSystem() == null) {
	    return initialize();
	}
	return Bennu.getInstance().getThesisProposalsSystem();
    }

    @Atomic(mode = TxMode.WRITE)
    private static ThesisProposalsSystem initialize() {
	if (Bennu.getInstance().getThesisProposalsSystem() == null) {
	    return new ThesisProposalsSystem();
	}
	return Bennu.getInstance().getThesisProposalsSystem();
    }

    public Stream<ThesisProposalsConfiguration> getThesisProposalsConfigurationByExecutionYear(ExecutionYear executionYear) {
	return getThesisProposalsConfigurationSet().stream().filter(
		p -> p.getExecutionDegree().getExecutionYear() == executionYear);
    }

    public Group getCanCreateThesisGroup() {
	return getThesisCreatorsGroup().toGroup();
    }

    @Atomic
    public void setCanManageGroup(Group group) {
	setThesisSystemManager(group.toPersistentGroup());
    }

    public Group getCanManageThesisGroup() {
	return getThesisSystemManager().toGroup();
    }

    @Atomic
    public void setCanCreateThesisGroup(Group group) {
	setThesisCreatorsGroup(group.toPersistentGroup());
    }

    public static boolean canManage(Degree degree, User user) {
	return DegreeHelper.getCanManageThesis(degree).isMember(user)
		|| ThesisProposalsSystem.getInstance().getCanManageThesisGroup().isMember(user);
    }
}