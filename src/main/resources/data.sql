use mini_fdu;

INSERT INTO `history_message` (`local_id`, `remote_id`, `content`, `time_stamp`, `type`)
VALUES (1, 2, 'Hello !', '2024-05-01 12:00:00', 'text');

INSERT INTO `history_message` (`local_id`, `remote_id`, `content`, `time_stamp`, `type`)
VALUES (2, 1, 'Hi!', '2024-05-01 12:05:00', 'text');

INSERT INTO `history_message` (`local_id`, `remote_id`, `content`, `time_stamp`, `type`)
VALUES (1, 2, 'How are you today?', '2024-05-01 12:10:00', 'text');
INSERT INTO `history_message` (`local_id`, `remote_id`, `content`, `time_stamp`, `type`)
VALUES (2, 3, '视频通话时长: 01:01:01', '2024-05-01 12:04:00', 'video');
INSERT INTO `history_message` (`local_id`, `remote_id`, `content`, `time_stamp`, `type`)
VALUES (2, 4, '视频通话时长: 02:05', '2024-05-04 12:08:00', 'video'),
       (2, 4, '视频通话：已拒绝', '2024-04-29 12:09:00', 'video'),
       (2, 1, '视频通话时长: 02:05', '2024-05-01 12:08:00', 'video'),
       (1, 2, '视频通话：已拒绝', '2024-05-01 12:09:00', 'video'),
       (2, 1, '视频通话：已拒绝', '2024-05-01 12:10:00', 'video');