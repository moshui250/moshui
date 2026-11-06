ALTER TABLE sys_user
    ADD COLUMN avatar VARCHAR(32) NOT NULL DEFAULT 'navy'
        COMMENT '头像配色：navy/slate/sage/sand/clay/ink' AFTER nickname;

UPDATE sys_user SET avatar = 'navy' WHERE avatar IS NULL OR avatar = '';
