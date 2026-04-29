ALTER TABLE posts ADD COLUMN sub_channel VARCHAR(50) NOT NULL DEFAULT '最新';

CREATE INDEX idx_posts_sub_channel ON posts(sub_channel);