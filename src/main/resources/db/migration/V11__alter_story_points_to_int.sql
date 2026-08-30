-- V11: Alter story_points from smallint to int to match Java Integer type

ALTER TABLE `todos` MODIFY COLUMN `story_points` int DEFAULT NULL;
