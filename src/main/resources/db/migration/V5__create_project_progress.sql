create table project_progress (
    id           bigint generated always as identity primary key,
    project_id   bigint       not null,
    period_year  int          not null,
    period_month int          not null,
    percentage   numeric(5,4) not null,
    constraint fk_progress_project foreign key (project_id) references projects (id),
    constraint uk_progress_project_period unique (project_id, period_year, period_month),
    constraint ck_progress_month check (period_month between 1 and 12),
    constraint ck_progress_percentage check (percentage > 0 and percentage <= 1)
);
