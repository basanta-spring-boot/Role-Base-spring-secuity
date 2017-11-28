package com.spring.rolebase.security.api.controller;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.spring.rolebase.security.api.dto.JobForm;
import com.spring.rolebase.security.api.model.JobDetails;
import com.spring.rolebase.security.api.model.User;
import com.spring.rolebase.security.api.service.UserService;

@Controller
public class PortalController {

	@Autowired
	private UserService userService;
	protected static String LOGGED_USER_ROLE = "";
	protected static int LOGGED_ID = 0;
	protected static User LOGGEDUSER = null;

	@RequestMapping(value = { "/", "/login" }, method = RequestMethod.GET)
	public ModelAndView login() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("login");
		return modelAndView;
	}

	@RequestMapping(value = "/registration", method = RequestMethod.GET)
	public ModelAndView registration() {
		ModelAndView modelAndView = new ModelAndView();
		User user = new User();
		modelAndView.addObject("user", user);
		modelAndView.setViewName("registration");
		return modelAndView;
	}

	@RequestMapping(value = "/registration", method = RequestMethod.POST)
	public ModelAndView createNewUser(@Valid User user,
			BindingResult bindingResult) {
		ModelAndView modelAndView = new ModelAndView();
		User userExists = userService.findUserByEmail(user.getEmail());
		if (userExists != null) {
			bindingResult
					.rejectValue("email", "error.user",
							"There is already a user registered with the email provided");
		}
		if (bindingResult.hasErrors()) {
			modelAndView.setViewName("registration");
		} else {
			userService.saveUser(user);
			modelAndView.addObject("successMessage",
					"User has been registered successfully");
			modelAndView.addObject("user", new User());
			modelAndView.setViewName("registration");

		}
		return modelAndView;
	}

	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public ModelAndView home() {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		User user = userService.findUserByEmail(auth.getName());
		LOGGEDUSER = user;
		modelAndView.addObject("userName", "Welcome " + user.getName() + " "
				+ user.getLastName() + " (" + user.getEmail() + ")");
		modelAndView.addObject("email", user.getEmail());
		String userRole = user.getRoles().stream().collect(Collectors.toList())
				.get(0).getRole();
		LOGGED_USER_ROLE = userRole;
		LOGGED_ID = user.getId();
		if (userRole.equalsIgnoreCase("ADMIN")) {
			modelAndView.addObject("adminMessage",
					"Content Available Only for Users with Admin Role");
			modelAndView.addObject("jobs", getJobPostedByLoggedUser());
			modelAndView.setViewName("admin/home");
		} else {
			modelAndView.addObject("UserMessage",
					"Content Available Only for Users with User Role");
			modelAndView.setViewName("user/home");
		}

		return modelAndView;
	}

	@RequestMapping(value = "/admin/addJobView")
	public String addJob(Model model) {
		User user = userService.findUserByEmail(LOGGEDUSER.getEmail());
		model.addAttribute("user", user);
		model.addAttribute("jobID", new Random().nextInt(3653956));
		return "admin/jobUpload";
	}

	@RequestMapping(value = "/admin/postJob")
	public String postJob(Model model, @ModelAttribute("form") JobForm form) {
		userService.saveJob(form, LOGGED_ID);
		model.addAttribute("userName", "Welcome " + LOGGEDUSER.getName() + " "
				+ LOGGEDUSER.getLastName() + " (" + LOGGEDUSER.getEmail() + ")");
		model.addAttribute("message", "Job Uploaded Successfully");
		model.addAttribute("jobs", getJobPostedByLoggedUser());
		return "admin/home";
	}

	@RequestMapping("/admin/deletePost")
	public String removeJob(Model model, @RequestParam("id") int id) {
		String deleteMessage = "";
		deleteMessage = userService.deleteJob(id);
		model.addAttribute("deleteMessage", deleteMessage);
		model.addAttribute("jobs", getJobPostedByLoggedUser());
		return "admin/home";

	}

	@RequestMapping("/admin/updatePostView")
	public String updateJobView(Model model, @RequestParam("id") int id) {
		JobDetails jobDetails = userService.getJobById(id);
		model.addAttribute("job", jobDetails);
		return "admin/updateJobUpload";
	}

	@RequestMapping("/admin/updatePost")
	public String updateJob(Model model, @ModelAttribute("form") JobForm form) {
		String updateMessage = "";
		updateMessage = userService.updateJob(form);
		model.addAttribute("updateMessage", updateMessage);
		model.addAttribute("jobs", getJobPostedByLoggedUser());
		return "admin/home";
	}

	private List<JobDetails> getJobPostedByLoggedUser() {
		List<JobDetails> jobDetails = userService.getJobsByPostUser(LOGGED_ID);
		return jobDetails;

	}
}
