package com.cst438.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentListDTO;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.AssignmentListDTO.AssignmentDTO;
import com.cst438.services.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.cst438.domain.CourseRepository;

@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001" })
@RestController
public class AssignmentController {

	@Autowired
	AssignmentRepository assignmentRepository;

	@Autowired
	AssignmentGradeRepository assignmentGradeRepository;

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	RegistrationService registrationService;

	// Needed to convert string -> date -> sql date (for setting dueDate)
	java.util.Date date;

	/* Try to get an AssignmentDTO object so I can see how it should look as JSON (DONE) */
	@GetMapping("course/{course_id}/assignment/{assignment_id}")
	public AssignmentListDTO.AssignmentDTO getAssignment(@PathVariable("course_id") int courseId,
			@PathVariable("assignment_id") int assignmentId) throws ResponseStatusException {

		// Find the assignment
		Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);

		// Double check that the assignment is for that course
		if (assignment.getCourse().getCourse_id() != courseId) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is not in the course.");
		}

		String date = assignment.getDueDate().toString();
		AssignmentDTO assignmentDTO = new AssignmentDTO(assignment.getId(), assignment.getCourse().getCourse_id(),
				assignment.getName(), date, assignment.getCourse().getTitle());

		return assignmentDTO;
	}

	/* Add an assignment with a name and due date (DONE) */
	@PostMapping("/course/{course_id}/assignment")
	@Transactional
	public AssignmentListDTO.AssignmentDTO addAssignment(@RequestBody AssignmentListDTO.AssignmentDTO assignmentDTO,
			@PathVariable("course_id") int courseId) throws ResponseStatusException, ParseException {
		// Check to make sure that the course exists
		Course course = courseRepository.findById(courseId).orElse(null);
		if (course == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid course.");
		}

		// Create new assignment with the course
		Assignment assignment = new Assignment();
		assignment.setCourse(course);

		// Date stuff to make nice with sql
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd"); // needed for the Date object
		long time = formatter.parse(assignmentDTO.dueDate).getTime();
		java.sql.Date date = new java.sql.Date(time); // convert to the sql date object

		// Add the two given parameters from the body
		assignment.setName(assignmentDTO.assignmentName);
		assignment.setDueDate(date);
		assignment.setNeedsGrading(1);

		// Save the assignment
		assignmentRepository.save(assignment);
		
		// Sanity check
		assignmentDTO.assignmentId = assignment.getId();
		assignmentDTO.courseTitle = course.getTitle();
		return assignmentDTO;
	}

	/* Update the name of an assignment (DONE) */
	@PutMapping("/course/{course_id}/assignment/{assignment_id}")
	@Transactional
	public void updateAssignmentName(@RequestBody AssignmentListDTO.AssignmentDTO assignmentDTO,
			@PathVariable("course_id") int courseId, @PathVariable("assignment_id") int assignmentId) {

		// Check to make sure that the assignment exists
		Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
		if (assignment == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assignment.");
		} else {
			assignment.setName(assignmentDTO.assignmentName); // Change name if found
		}

		assignmentRepository.save(assignment);
	}

	/* Delete an assignment if there are no grades for it (DONE) */
	@DeleteMapping("/course/{course_id}/assignment/{assignment_id}")
	@Transactional
	public void deleteAssignment(@PathVariable("assignment_id") int assignmentId) {

		// Find the assignment
		Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);

		if (assignment == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assignment.");
		} else {
			if (assignment.getNeedsGrading() == 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Assignment already has grades. Cannot delete.");
			} else {
				assignmentRepository.delete(assignment);
			}
		}
	}
}