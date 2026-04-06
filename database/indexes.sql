-- Optional index improvements for larger datasets

CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_projects_deadline ON projects(deadline);
CREATE INDEX IF NOT EXISTS idx_weekly_revenue_week_no ON weekly_revenue_history(week_no DESC);
