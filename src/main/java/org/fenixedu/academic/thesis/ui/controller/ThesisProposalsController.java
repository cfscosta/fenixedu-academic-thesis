/**
 * Copyright © 2014 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic Thesis.
 *
 * FenixEdu Academic Thesis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic Thesis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic Thesis.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.thesis.ui.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.accessControl.CoordinatorGroup;
import org.fenixedu.academic.thesis.domain.StudentThesisCandidacy;
import org.fenixedu.academic.thesis.domain.ThesisProposal;
import org.fenixedu.academic.thesis.domain.ThesisProposalParticipant;
import org.fenixedu.academic.thesis.domain.ThesisProposalsConfiguration;
import org.fenixedu.academic.thesis.ui.bean.ThesisProposalBean;
import org.fenixedu.academic.thesis.ui.bean.ThesisProposalParticipantBean;
import org.fenixedu.academic.thesis.ui.exception.CannotEditUsedThesisProposalsException;
import org.fenixedu.academic.thesis.ui.exception.OutOfProposalPeriodException;
import org.fenixedu.academic.thesis.ui.exception.ThesisProposalException;
import org.fenixedu.academic.thesis.ui.exception.UnequivalentThesisConfigurationsException;
import org.fenixedu.academic.thesis.ui.exception.UnexistentConfigurationException;
import org.fenixedu.academic.thesis.ui.service.ThesisProposalsService;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pt.ist.fenixframework.FenixFramework;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

@SpringApplication(group = "thesisCreators | activeStudents | #managers", path = "thesisProposals",
        title = "application.title.thesis", hint = "Thesis")
@SpringFunctionality(app = ThesisProposalsController.class, title = "title.thesisProposal.management",
        accessGroup = "thesisSystemManagers | thesisCreators")
@RequestMapping("/proposals")
public class ThesisProposalsController {

    @Autowired
    ThesisProposalsService service;

    private String listProposals(Model model, ThesisProposalsConfiguration configuration) {
        return listProposals(model, configuration, null, null, null);
    }

    @RequestMapping(method = RequestMethod.GET)
    public String listProposals(Model model, @RequestParam(required = false) ThesisProposalsConfiguration configuration,
            @RequestParam(required = false) Boolean isVisible, @RequestParam(required = false) Boolean isAttributed,
            @RequestParam(required = false) Boolean hasCandidacy) {

        List<ThesisProposalsConfiguration> configs = service.getThesisProposalsConfigurations(Authenticate.getUser());

        if (configuration == null && !configs.isEmpty()) {
            configuration = configs.iterator().next();
        }

        if (configuration == null) {
            model.addAttribute("error", "cant.manage.list.proposals");
            return "proposals/list";
        }

        model.addAttribute("service", service);
        model.addAttribute("configurations", configs);
        model.addAttribute("configuration", configuration);
        model.addAttribute("thesisProposalsList", service.getThesisProposals(Authenticate.getUser(), configuration));

        return "proposals/list";
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public ModelAndView createThesisProposalsForm(Model model) {

        ModelAndView modelAndview = new ModelAndView("proposals/create", "command", new ThesisProposalBean());

        modelAndview.addObject("configurations", service
                .getCurrentThesisProposalsConfigurations(ThesisProposalsConfiguration.COMPARATOR_BY_YEAR_AND_EXECUTION_DEGREE));

        modelAndview.addObject("participantTypeList", service.getThesisProposalParticipantTypes());

        modelAndview.addObject("action", "proposals/create");

        return modelAndview;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ModelAndView createThesisProposals(@ModelAttribute ThesisProposalBean proposalBean,
            @RequestParam String participantsJson, @RequestParam Set<ThesisProposalsConfiguration> thesisProposalsConfigurations,
            Model model) {

        try {
            if (thesisProposalsConfigurations == null || thesisProposalsConfigurations.isEmpty()) {
                throw new UnexistentConfigurationException();
            }

            ThesisProposalsConfiguration base = thesisProposalsConfigurations.iterator().next();

            for (ThesisProposalsConfiguration configuration : thesisProposalsConfigurations) {
                if (!base.isEquivalent(configuration)) {
                    throw new UnequivalentThesisConfigurationsException(base, configuration);
                }
            }

            if (!base.getProposalPeriod().containsNow()) {
                throw new OutOfProposalPeriodException();
            }

            proposalBean.setThesisProposalsConfigurations(thesisProposalsConfigurations);
            service.createThesisProposal(proposalBean, participantsJson);
        } catch (ThesisProposalException exception) {
            model.addAttribute("error", exception.getClass().getSimpleName());
            return error(proposalBean, model);
        }

        return new ModelAndView(listProposals(model, null));
    }

    protected ModelAndView error(ThesisProposalBean proposalBean, Model model) {
        model.addAttribute("configurations", service.getCurrentThesisProposalsConfigurations());
        model.addAttribute("participantTypeList", service.getThesisProposalParticipantTypes());
        model.addAttribute("command", proposalBean);
        return new ModelAndView("proposals/create", model.asMap());
    }

    @RequestMapping(value = "/delete/{oid}", method = RequestMethod.POST)
    public ModelAndView deleteThesisProposals(@PathVariable("oid") ThesisProposal thesisProposal, Model model) {

        if (!service.delete(thesisProposal)) {
            model.addAttribute("deleteException", true);
            return new ModelAndView(listProposals(model, null));
        }

        return new ModelAndView("redirect:/proposals");
    }

    @RequestMapping(value = "/edit/{oid}", method = RequestMethod.GET)
    public ModelAndView editProposalForm(@PathVariable("oid") ThesisProposal thesisProposal,
            @RequestParam(required = false) ThesisProposalsConfiguration configuration, Model model) {

        boolean isManager = DynamicGroup.get("managers").isMember(Authenticate.getUser());
        boolean isDegreeCoordinator =
                thesisProposal.getExecutionDegreeSet().stream()
                        .anyMatch(execDegree -> CoordinatorGroup.get(execDegree.getDegree()).isMember(Authenticate.getUser()));

        if (configuration == null) {
            configuration = thesisProposal.getSingleThesisProposalsConfiguration();
        }

        model.addAttribute("configuration", configuration);
        model.addAttribute("action", "proposals/edit");
        try {
            if (isDegreeCoordinator
                    || (isManager || thesisProposal.getSingleThesisProposalsConfiguration().getProposalPeriod()
                            .contains(DateTime.now()))) {
                if (!thesisProposal.getStudentThesisCandidacySet().isEmpty()) {
                    throw new CannotEditUsedThesisProposalsException(thesisProposal);
                } else {
                    HashSet<ThesisProposalParticipantBean> thesisProposalParticipantsBean =
                            new HashSet<ThesisProposalParticipantBean>();

                    for (ThesisProposalParticipant participant : thesisProposal.getThesisProposalParticipantSet()) {

                        String participantType = participant.getThesisProposalParticipantType().getExternalId();

                        ThesisProposalParticipantBean bean =
                                new ThesisProposalParticipantBean(participant.getUser(), participantType);

                        thesisProposalParticipantsBean.add(bean);
                    }

                    ThesisProposalBean thesisProposalBean =
                            new ThesisProposalBean(thesisProposal.getTitle(), thesisProposal.getObservations(),
                                    thesisProposal.getRequirements(), thesisProposal.getGoals(),
                                    thesisProposal.getLocalization(), thesisProposal.getThesisConfigurationSet(),
                                    thesisProposal.getStudentThesisCandidacySet(), thesisProposalParticipantsBean,
                                    thesisProposal.getHidden(), thesisProposal.getExternalId());

                    ModelAndView mav = new ModelAndView("proposals/edit", "command", thesisProposalBean);

                    mav.addObject("configurations", service.getCurrentThesisProposalsConfigurations());

                    mav.addObject("participantTypeList", service.getThesisProposalParticipantTypes());

                    return mav;
                }
            } else {
                throw new OutOfProposalPeriodException();
            }
        } catch (ThesisProposalException exception) {
            model.addAttribute("error", exception.getClass().getSimpleName());
            return new ModelAndView(listProposals(model, configuration));
        }
    }

    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ModelAndView editProposal(@ModelAttribute ThesisProposalBean thesisProposalBean,
            @RequestParam String participantsJson, @RequestParam(required = false) ThesisProposalsConfiguration configuration,
            @RequestParam Set<ThesisProposalsConfiguration> thesisProposalsConfigurations, Model model,
            RedirectAttributes redirectAttrs) {

        thesisProposalBean.setThesisProposalsConfigurations(thesisProposalsConfigurations);

        ThesisProposal thesisProposal = FenixFramework.getDomainObject(thesisProposalBean.getExternalId());

        JsonParser parser = new JsonParser();
        JsonArray jsonArray = (JsonArray) parser.parse(participantsJson);

        try {
            service.editThesisProposal(Authenticate.getUser(), thesisProposalBean, thesisProposal, jsonArray);
            redirectAttrs.addAttribute("configuration", configuration != null ? configuration.getExternalId() : null);
            return new ModelAndView("redirect:/proposals");
        } catch (ThesisProposalException exception) {
            model.addAttribute("error", exception.getClass().getSimpleName());
            return editProposalForm(thesisProposal, configuration, model);
        }
    }

    @RequestMapping(value = "/accept/{studentThesisCandidacy}", method = RequestMethod.POST)
    public String acceptStudentThesisCandidacy(
            @PathVariable("studentThesisCandidacy") StudentThesisCandidacy studentThesisCandidacy) {
        service.accept(studentThesisCandidacy);
        return "redirect:/proposals/manage/" + studentThesisCandidacy.getThesisProposal().getExternalId();
    }

    @RequestMapping(value = "/reject/{studentThesisCandidacy}", method = RequestMethod.POST)
    public String rejectStudentThesisCandidacy(
            @PathVariable("studentThesisCandidacy") StudentThesisCandidacy studentThesisCandidacy) {
        service.reject(studentThesisCandidacy);
        return "redirect:/proposals/manage/" + studentThesisCandidacy.getThesisProposal().getExternalId();
    }

    @RequestMapping(value = "/manage/{oid}", method = RequestMethod.GET)
    public ModelAndView manageCandidacies(@PathVariable("oid") ThesisProposal thesisProposal, Model model) {
        ModelAndView view = new ModelAndView("thesisCandidacies/manage");
        view.addObject("thesisProposal", thesisProposal);
        view.addObject("action", "proposals/accept");
        view.addObject("candidaciesList", service.getStudentThesisCandidacy(thesisProposal));
        view.addObject("bestAccepted", service.getBestAccepted(thesisProposal));
        return view;
    }

    @RequestMapping(value = "/transpose", method = RequestMethod.GET)
    public String listOldProposals(Model model) {
        model.addAttribute("recentProposals", service.getRecentProposals(Authenticate.getUser()));
        return "proposals/old";
    }

    @RequestMapping(value = "/transpose/{oid}", method = RequestMethod.GET)
    public ModelAndView transposeProposal(@PathVariable("oid") ThesisProposal thesisProposal, Model model) {
        ThesisProposalBean proposalBean = new ThesisProposalBean(thesisProposal);
        ModelAndView view = new ModelAndView("proposals/create", "command", proposalBean);
        view.addObject("configurations", service.getCurrentThesisProposalsConfigurations());
        view.addObject("participantTypeList", service.getThesisProposalParticipantTypes());
        return view;
    }

}
