package org.fenixedu.academic.thesis.ui.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jvstm.cps.ConsistencyException;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.thesis.domain.ThesisProposal;
import org.fenixedu.academic.thesis.domain.ThesisProposalParticipantType;
import org.fenixedu.academic.thesis.domain.ThesisProposalsConfiguration;
import org.fenixedu.academic.thesis.domain.ThesisProposalsSystem;
import org.fenixedu.academic.thesis.ui.bean.ConfigurationBean;
import org.fenixedu.academic.thesis.ui.bean.ParticipantTypeBean;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SpringFunctionality(app = ThesisProposalsController.class, title = "title.configuration.management", accessGroup = "#managers | thesisSystemManagers")
@RequestMapping("/configuration")
public class ConfigurationController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String listThesisProposalsConfigurationForm(Model model) {

	TreeSet<ExecutionYear> executionYearsList = new TreeSet<ExecutionYear>(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR);
	executionYearsList.addAll(Bennu.getInstance().getExecutionYearsSet());

	model.addAttribute("executionYearsList", executionYearsList);

	Set<ThesisProposalsConfiguration> configurationsSet = ThesisProposalsSystem.getInstance()
		.getThesisProposalsConfigurationSet();

	List<ThesisProposalsConfiguration> configurationsList = configurationsSet.stream()
		.filter((x) -> ThesisProposalsSystem.canManage(x.getExecutionDegree().getDegree(), Authenticate.getUser()))
		.collect(Collectors.toList());
	Collections.sort(configurationsList, ThesisProposalsConfiguration.COMPARATOR_BY_YEAR_AND_EXECUTION_DEGREE);

	model.addAttribute("configurationsList", configurationsList);

	List<ExecutionDegree> executionDegreeList = Bennu.getInstance().getExecutionDegreesSet().stream()
		.filter((x) -> ThesisProposalsSystem.canManage(x.getDegree(), Authenticate.getUser()))
		.collect(Collectors.toList());

	Collections.sort(executionDegreeList, ExecutionDegree.COMPARATOR_BY_DEGREE_NAME);

	model.addAttribute("executionDegreeList", executionDegreeList);

	List<ThesisProposalParticipantType> participantTypeList = ThesisProposalsSystem.getInstance()
		.getThesisProposalParticipantTypeSet().stream().collect(Collectors.toList());

	Collections.sort(participantTypeList, ThesisProposalParticipantType.COMPARATOR_BY_WEIGHT);

	model.addAttribute("participantTypeList", participantTypeList);

	model.addAttribute("isManager", DynamicGroup.get("managers").isMember(Authenticate.getUser()));

	return "/configuration/list";
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public ModelAndView createThesisProposalsConfigurationForm(Model model,
	    @RequestParam ConfigurationBean thesisProposalsConfigurationBean) {

	ModelAndView mav = new ModelAndView("/configuration/create", "command", thesisProposalsConfigurationBean);

	TreeSet<ExecutionYear> executionYearsList = new TreeSet<ExecutionYear>(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR);
	executionYearsList.addAll(Bennu.getInstance().getExecutionYearsSet());
	model.addAttribute("executionYearsList", executionYearsList);

	List<ExecutionDegree> executionDegreeList = Bennu.getInstance().getExecutionDegreesSet().stream()
		.collect(Collectors.toList());
	Collections.sort(executionDegreeList, ExecutionDegree.COMPARATOR_BY_DEGREE_NAME);
	mav.addObject("executionDegreeList", executionDegreeList);

	return mav;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ModelAndView createThesisProposals(@ModelAttribute ConfigurationBean configurationBean, Model model) {

	try {
	    if (configurationBean.getExecutionDegree() == null) {
		model.addAttribute("unselectedExecutionDegreeException", true);
		return createThesisProposalsConfigurationForm(model, configurationBean);
	    }

	    new ConfigurationBean.Builder(configurationBean).build();
	} catch (ConsistencyException exception) {
	    model.addAttribute("createException", true);
	    model.addAttribute("command", configurationBean);
	    model.addAttribute("executionDegreeList", ThesisProposal.getThesisExecutionDegrees());

	    TreeSet<ExecutionYear> executionYearsList = new TreeSet<ExecutionYear>(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR);
	    executionYearsList.addAll(Bennu.getInstance().getExecutionYearsSet());
	    model.addAttribute("executionYearsList", executionYearsList);

	    return new ModelAndView("/configuration/create", model.asMap());
	} catch (IllegalArgumentException exception) {
	    model.addAttribute("illegalArgumentException", true);
	    model.addAttribute("command", configurationBean);
	    model.addAttribute("executionDegreeList", ThesisProposal.getThesisExecutionDegrees());

	    TreeSet<ExecutionYear> executionYearsList = new TreeSet<ExecutionYear>(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR);
	    executionYearsList.addAll(Bennu.getInstance().getExecutionYearsSet());
	    model.addAttribute("executionYearsList", executionYearsList);

	    return new ModelAndView("/configuration/create", model.asMap());
	}

	return new ModelAndView("redirect:/configuration");
    }

    @RequestMapping(value = "/delete/{oid}", method = RequestMethod.POST)
    public ModelAndView deleteThesisProposals(@PathVariable("oid") ThesisProposalsConfiguration thesisProposalsConfiguration,
	    Model model) {

	try {
	    delete(thesisProposalsConfiguration);
	} catch (DomainException exception) {
	    model.addAttribute("deleteException", true);
	    return editConfigurationForm(thesisProposalsConfiguration, model);
	}

	return new ModelAndView(listThesisProposalsConfigurationForm(model));
    }

    @Atomic(mode = TxMode.WRITE)
    private void delete(ThesisProposalsConfiguration thesisProposalsConfiguration) {
	thesisProposalsConfiguration.delete();
    }

    @RequestMapping(value = "/edit/{oid}", method = RequestMethod.GET)
    public ModelAndView editConfigurationForm(@PathVariable("oid") ThesisProposalsConfiguration configuration, Model model) {

	ConfigurationBean configurationBean = new ConfigurationBean(configuration.getProposalPeriod().getStart(), configuration
		.getProposalPeriod().getEnd(), configuration.getCandidacyPeriod().getStart(), configuration.getCandidacyPeriod()
		.getEnd(), configuration.getExecutionDegree(), configuration.getExternalId(),
		configuration.getMaxThesisCandidaciesByStudent(), configuration.getMaxThesisProposalsByUser());

	ModelAndView mav = new ModelAndView("configuration/edit", "command", configurationBean);

	return mav;
    }

    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ModelAndView editConfiguration(@ModelAttribute ConfigurationBean configurationBean, Model model) {

	try {
	    edit(configurationBean);
	} catch (IllegalArgumentException exception) {
	    model.addAttribute("illegalArgumentException", true);
	    model.addAttribute("command", configurationBean);
	    model.addAttribute("executionDegreeList", ThesisProposal.getThesisExecutionDegrees());

	    TreeSet<ExecutionYear> executionYearsList = new TreeSet<ExecutionYear>(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR);
	    executionYearsList.addAll(Bennu.getInstance().getExecutionYearsSet());
	    model.addAttribute("executionYearsList", executionYearsList);

	    return new ModelAndView("/configuration/edit", model.asMap());
	}

	return new ModelAndView("redirect:/configuration");
    }

    @Atomic(mode = TxMode.WRITE)
    private void edit(ConfigurationBean configurationBean) {
	ThesisProposalsConfiguration thesisProposalsConfiguration = FenixFramework.getDomainObject(configurationBean
		.getExternalId());

	DateTimeFormatter formatter = ISODateTimeFormat.dateTime();

	DateTime proposalPeriodStartDT = formatter.parseDateTime(configurationBean.getProposalPeriodStart());
	DateTime proposalPeriodEndDT = formatter.parseDateTime(configurationBean.getProposalPeriodEnd());
	DateTime candidacyPeriodStartDT = formatter.parseDateTime(configurationBean.getCandidacyPeriodStart());
	DateTime candidacyPeriodEndDT = formatter.parseDateTime(configurationBean.getCandidacyPeriodEnd());

	Interval proposalPeriod = new Interval(proposalPeriodStartDT, proposalPeriodEndDT);
	Interval candidacyPeriod = new Interval(candidacyPeriodStartDT, candidacyPeriodEndDT);

	thesisProposalsConfiguration.setProposalPeriod(proposalPeriod);
	thesisProposalsConfiguration.setCandidacyPeriod(candidacyPeriod);

	thesisProposalsConfiguration.setMaxThesisCandidaciesByStudent(configurationBean.getMaxThesisCandidaciesByStudent());
	thesisProposalsConfiguration.setMaxThesisProposalsByUser(configurationBean.getMaxThesisProposalsByUser());
    }

    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/execution-year/{executionYear}/execution-degrees", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<String> getExecutionDegreesByYear(
	    @PathVariable("executionYear") ExecutionYear executionYear) {

	JsonArray response = new JsonArray();

	List<ExecutionDegree> executionDegreeList = ExecutionDegree.getAllByExecutionYear(executionYear).stream()
		.filter((x) -> ThesisProposalsSystem.canManage(x.getDegree(), Authenticate.getUser()))
		.collect(Collectors.toList());

	Collections.sort(executionDegreeList,
		ExecutionDegree.EXECUTION_DEGREE_COMPARATORY_BY_DEGREE_TYPE_AND_NAME_AND_EXECUTION_YEAR);

	executionDegreeList.forEach(executionDegree -> response.add(executionDegreeToJson(executionDegree)));

	return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
    }

    private JsonElement executionDegreeToJson(ExecutionDegree executionDegree) {
	JsonObject json = new JsonObject();

	json.addProperty("externalId", executionDegree.getExternalId());
	json.addProperty("name", executionDegree.getPresentationName());

	return json;
    }

    @RequestMapping(value = "createParticipantType", method = RequestMethod.GET)
    public String createParticipantTypeForm(Model model) {

	List<ThesisProposalParticipantType> participantTypeList = ThesisProposalsSystem.getInstance()
		.getThesisProposalParticipantTypeSet().stream().collect(Collectors.toList());

	Collections.sort(participantTypeList, ThesisProposalParticipantType.COMPARATOR_BY_WEIGHT);

	model.addAttribute("participantTypeList", participantTypeList);

	return "participantsType/create";
    }

    @RequestMapping(value = "/createParticipantType", method = RequestMethod.POST)
    public String createParticipantType(@RequestParam LocalizedString name, @RequestParam int weight) {

	createThesisProposalParticipantType(name, weight);

	return "redirect:/configuration";
    }

    @Atomic(mode = TxMode.WRITE)
    public void createThesisProposalParticipantType(LocalizedString name, int weight) {
	new ThesisProposalParticipantType(name, weight);
    }

    @RequestMapping(value = "deleteParticipantType/{participantType}", method = RequestMethod.POST)
    public String deleteParticipantType(@PathVariable("participantType") ThesisProposalParticipantType participantType,
	    Model model) {

	return delete(participantType, model);
    }

    @Atomic(mode = TxMode.WRITE)
    private String delete(ThesisProposalParticipantType participantType, Model model) {
	try {
	    participantType.delete();
	} catch (DomainException domainException) {
	    model.addAttribute("deleteException", true);
	    return createParticipantTypeForm(model);
	}

	return "redirect:/configuration";
    }

    @RequestMapping(value = "editParticipantType/{participantType}", method = RequestMethod.GET)
    public ModelAndView editParticipantTypeForm(@PathVariable("participantType") ThesisProposalParticipantType participantType,
	    Model model) {

	if (participantType.getThesisProposalParticipantSet().size() > 0) {
	    model.addAttribute("editException", true);
	    return new ModelAndView(createParticipantTypeForm(model));
	} else {
	    ParticipantTypeBean thesisProposalParticipantTypeBean = new ParticipantTypeBean(participantType.getName(),
		    participantType.getWeight(), participantType.getExternalId());

	    ModelAndView mav = new ModelAndView("participantsType/edit", "command", thesisProposalParticipantTypeBean);
	    return mav;
	}
    }

    @RequestMapping(value = "editParticipantType", method = RequestMethod.POST)
    public String editParticipantType(@RequestParam LocalizedString name, @RequestParam String externalId,
	    @RequestParam int weight) {

	ParticipantTypeBean bean = new ParticipantTypeBean(name, weight, externalId);

	return edit(bean);
    }

    @Atomic(mode = TxMode.WRITE)
    private String edit(ParticipantTypeBean participantTypeBean) {
	ThesisProposalParticipantType thesisProposalParticipantType = FenixFramework.getDomainObject(participantTypeBean
		.getExternalId());

	thesisProposalParticipantType.setName(participantTypeBean.getName());

	return "redirect:/configuration";
    }

    @RequestMapping(value = "/updateWeights", method = RequestMethod.POST)
    public String updateParticipantTypeWeights(@RequestParam String json) {

	JsonParser parser = new JsonParser();
	JsonArray jsonArray = (JsonArray) parser.parse(json);

	updateParticipantTypeWeights(jsonArray);

	return "redirect:/configuration";
    }

    @Atomic(mode = TxMode.WRITE)
    public void updateParticipantTypeWeights(JsonArray jsonArray) {
	jsonArray.forEach((JsonElement elem) -> {
	    String externalId = elem.getAsJsonObject().get("externalId").getAsString();
	    int weight = elem.getAsJsonObject().get("weight").getAsInt();

	    ThesisProposalParticipantType type = FenixFramework.getDomainObject(externalId);
	    type.setWeight(weight);
	});
    }

}