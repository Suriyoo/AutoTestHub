USE autotest_hub;

CREATE TABLE IF NOT EXISTS api_execution_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
    case_id BIGINT NOT NULL COMMENT 'API用例ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    environment_id BIGINT DEFAULT NULL COMMENT '执行环境ID',

    case_name VARCHAR(150) NOT NULL COMMENT '用例名称快照',
    environment_name VARCHAR(64) DEFAULT NULL COMMENT '环境名称快照',

    method VARCHAR(10) NOT NULL COMMENT '请求方法',
    request_url VARCHAR(1500) DEFAULT NULL COMMENT '最终请求URL',
    request_headers LONGTEXT COMMENT '请求Headers',
    request_params LONGTEXT COMMENT '请求Params',
    request_body LONGTEXT COMMENT '请求Body',

    success TINYINT NOT NULL DEFAULT 0 COMMENT '请求是否成功',
    passed TINYINT NOT NULL DEFAULT 0 COMMENT '断言是否通过',
    expected_status INT DEFAULT NULL COMMENT '预期状态码',
    actual_status INT DEFAULT NULL COMMENT '实际状态码',

    duration BIGINT DEFAULT NULL COMMENT '执行耗时毫秒',
    response_body LONGTEXT COMMENT '响应内容',
    error_message TEXT COMMENT '错误信息',

    executed_by BIGINT NOT NULL COMMENT '执行用户ID',
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',

    PRIMARY KEY (id),
    KEY idx_record_case (case_id),
    KEY idx_record_project (project_id),
    KEY idx_record_environment (environment_id),
    KEY idx_record_executed_at (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='API用例执行历史记录';
