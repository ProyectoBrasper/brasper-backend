create table project_cost_estimates (
    id               bigint generated always as identity primary key,
    project_id       bigint        not null,
    category         varchar(20)   not null,
    estimated_amount numeric(14,2) not null,
    constraint fk_cost_estimate_project foreign key (project_id) references projects (id),
    constraint uk_cost_estimate_project_category unique (project_id, category),
    constraint ck_cost_estimate_category check (category in ('MATERIALS', 'LABOR', 'TRAVEL_EXPENSES', 'OTHER')),
    constraint ck_cost_estimate_amount check (estimated_amount >= 0)
);
