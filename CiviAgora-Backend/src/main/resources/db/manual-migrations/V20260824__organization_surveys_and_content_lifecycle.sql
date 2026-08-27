-- CIVOX organization product upgrade (MySQL 8+).
-- Apply through the deployment migration runner before disabling Hibernate ddl-auto=update.

CREATE TABLE IF NOT EXISTS organization_surveys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id INT NOT NULL,
    created_by_user_id INT NOT NULL,
    title VARCHAR(180) NOT NULL,
    description LONGTEXT NULL,
    status VARCHAR(30) NOT NULL,
    opening_at DATETIME(6) NULL,
    closing_at DATETIME(6) NULL,
    result_visibility VARCHAR(30) NOT NULL,
    featured BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_survey_org_status (organization_id, status),
    CONSTRAINT fk_survey_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_survey_creator FOREIGN KEY (created_by_user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS organization_survey_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    survey_id BIGINT NOT NULL,
    position INT NOT NULL,
    prompt VARCHAR(500) NOT NULL,
    type VARCHAR(30) NOT NULL,
    required BIT NOT NULL DEFAULT 0,
    options_text LONGTEXT NULL,
    PRIMARY KEY (id),
    INDEX idx_survey_question_survey (survey_id),
    CONSTRAINT fk_survey_question_survey FOREIGN KEY (survey_id) REFERENCES organization_surveys(id)
);

CREATE TABLE IF NOT EXISTS organization_survey_submissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id INT NOT NULL,
    survey_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    submitted_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_survey_submission_user (survey_id, user_id),
    INDEX idx_survey_submission_organization (organization_id),
    CONSTRAINT fk_survey_submission_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_survey_submission_survey FOREIGN KEY (survey_id) REFERENCES organization_surveys(id),
    CONSTRAINT fk_survey_submission_user FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS organization_survey_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    value_text LONGTEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_survey_answer_question (submission_id, question_id),
    INDEX idx_survey_answer_question (question_id),
    CONSTRAINT fk_survey_answer_submission FOREIGN KEY (submission_id) REFERENCES organization_survey_submissions(id),
    CONSTRAINT fk_survey_answer_question FOREIGN KEY (question_id) REFERENCES organization_survey_questions(id)
);

ALTER TABLE organization_content_item
    ADD COLUMN IF NOT EXISTS opening_at DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS closing_at DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS result_visibility VARCHAR(30) NULL,
    ADD COLUMN IF NOT EXISTS featured BIT NULL;

UPDATE organization_content_item
SET result_visibility = 'AFTER_RESPONSE'
WHERE result_visibility IS NULL;

UPDATE organization_content_item
SET featured = 0
WHERE featured IS NULL;
