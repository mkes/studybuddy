-- Create planner_items table for storing Canvas assignment data
-- This table stores assignment information for students including grades, due dates, and submission status

CREATE TABLE planner_items (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    plannable_id BIGINT NOT NULL,
    assignment_title VARCHAR(500) NOT NULL,
    context_name VARCHAR(200),
    due_at TIMESTAMP,
    points_possible DECIMAL(10,2),
    current_grade DECIMAL(10,2),
    submitted BOOLEAN NOT NULL DEFAULT FALSE,
    missing BOOLEAN NOT NULL DEFAULT FALSE,
    late BOOLEAN NOT NULL DEFAULT FALSE,
    graded BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure unique combination of student and assignment
    CONSTRAINT uk_planner_items_student_plannable UNIQUE (student_id, plannable_id)
);

-- Create index for efficient querying by student and due date (descending order)
CREATE INDEX idx_planner_items_student_due_desc 
ON planner_items(student_id, due_at DESC);

-- Create index for efficient querying by student only
CREATE INDEX idx_planner_items_student_id 
ON planner_items(student_id);

-- Create index for efficient querying by due date for reporting
CREATE INDEX idx_planner_items_due_at 
ON planner_items(due_at);

-- Add comments for documentation
COMMENT ON TABLE planner_items IS 'Stores Canvas LMS assignment data for students including grades and submission status';
COMMENT ON COLUMN planner_items.student_id IS 'Canvas student ID from the API';
COMMENT ON COLUMN planner_items.plannable_id IS 'Canvas assignment/plannable ID from the API';
COMMENT ON COLUMN planner_items.assignment_title IS 'Title of the assignment from Canvas';
COMMENT ON COLUMN planner_items.context_name IS 'Course name or context from Canvas';
COMMENT ON COLUMN planner_items.due_at IS 'Assignment due date and time';
COMMENT ON COLUMN planner_items.points_possible IS 'Maximum points possible for the assignment';
COMMENT ON COLUMN planner_items.current_grade IS 'Current grade/score for the assignment';
COMMENT ON COLUMN planner_items.submitted IS 'Whether the assignment has been submitted';
COMMENT ON COLUMN planner_items.missing IS 'Whether the assignment is marked as missing in Canvas';
COMMENT ON COLUMN planner_items.late IS 'Whether the assignment was submitted late';
COMMENT ON COLUMN planner_items.graded IS 'Whether the assignment has been graded';