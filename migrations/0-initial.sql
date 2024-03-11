-- MariaDB dump 10.19  Distrib 10.6.16-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: c_stage_db_1
-- ------------------------------------------------------
-- Server version	10.6.16-MariaDB-0ubuntu0.22.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `actions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `actions` (
  `type` varchar(31) NOT NULL,
  `action_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(45) NOT NULL,
  `scope_reference_id` varchar(255) DEFAULT NULL,
  `scope_type` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `priority` varchar(45) DEFAULT NULL,
  `content_template` text DEFAULT NULL,
  `ticket_action_id` varchar(45) DEFAULT NULL,
  `group_id` varchar(45) DEFAULT NULL,
  `call_mode` varchar(45) DEFAULT NULL,
  `call_type` varchar(45) DEFAULT NULL,
  `mime_type` varchar(127) DEFAULT NULL,
  `num_retries` int(11) DEFAULT NULL,
  `payload_template` text DEFAULT NULL,
  `retry_strategy` varchar(45) DEFAULT NULL,
  `success_codes` varchar(127) DEFAULT NULL,
  `timeout_ms` int(11) DEFAULT NULL,
  `url_template` text DEFAULT NULL,
  `field_schema_id` varchar(139) DEFAULT NULL,
  `boolean_value` bit(1) DEFAULT NULL,
  `choices_value` varchar(512) DEFAULT NULL,
  `date_value` datetime DEFAULT NULL,
  `location_lat_value` double DEFAULT NULL,
  `location_lon_value` double DEFAULT NULL,
  `number_value` double DEFAULT NULL,
  `string_value` varchar(512) DEFAULT NULL,
  `field_type` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`action_id`),
  KEY `idx_scope_reference_id` (`scope_reference_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `addresses`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `addresses` (
  `address_id` varchar(45) NOT NULL,
  `city` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `house_number` varchar(255) DEFAULT NULL,
  `locality` varchar(255) DEFAULT NULL,
  `pin_code` varchar(255) DEFAULT NULL,
  `state` varchar(255) DEFAULT NULL,
  `street` varchar(255) DEFAULT NULL,
  `subject_global_id` varchar(45) NOT NULL,
  `type` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`address_id`),
  KEY `idx_subject_global_id` (`subject_global_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attribute_definitions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `attribute_definitions` (
  `type` varchar(31) NOT NULL,
  `attribute_def_id` varchar(255) NOT NULL,
  `created` timestamp NOT NULL DEFAULT current_timestamp(),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `scope_type` varchar(255) DEFAULT NULL,
  `updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `max` double DEFAULT NULL,
  `min` double DEFAULT NULL,
  `allow_multiple` bit(1) DEFAULT NULL,
  `options` varchar(255) DEFAULT NULL,
  `max_length` int(11) DEFAULT NULL,
  `pattern` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`attribute_def_id`),
  KEY `idx_attr_def_scope_type` (`scope_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attribute_values`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `attribute_values` (
  `type` varchar(31) NOT NULL,
  `attribute_value_id` varchar(255) NOT NULL,
  `attr_def_id` varchar(255) DEFAULT NULL,
  `attribute_id` varchar(255) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT current_timestamp(),
  `deleted` bit(1) DEFAULT NULL,
  `object_ref_id` varchar(255) DEFAULT NULL,
  `scope_type` varchar(255) DEFAULT NULL,
  `updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `link_text` varchar(255) DEFAULT NULL,
  `text_value` varchar(255) DEFAULT NULL,
  `date_value` datetime DEFAULT NULL,
  `number_value` double DEFAULT NULL,
  PRIMARY KEY (`attribute_value_id`),
  KEY `idx_attr_val_scope` (`scope_type`,`object_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dashboards`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dashboards` (
  `dashboard_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `provisioned_by` varchar(30) DEFAULT NULL,
  `name` varchar(45) DEFAULT NULL,
  `spec` text DEFAULT NULL,
  `spec_version` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`dashboard_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `events`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `events` (
  `id` varchar(255) NOT NULL,
  `date` datetime(3) DEFAULT current_timestamp(3),
  `referred_object_id` varchar(255) DEFAULT NULL,
  `referred_object_type` varchar(45) DEFAULT NULL,
  `partition_id` int(11) DEFAULT NULL,
  `source` longtext DEFAULT NULL,
  `source_fmt` char(3) DEFAULT NULL,
  `type` varchar(45) DEFAULT NULL,
  `user_id` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_id_partition` (`id`,`partition_id`),
  KEY `idx_reference_type_reference_id` (`referred_object_type`,`referred_object_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `field_schemas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `field_schemas` (
  `type` varchar(31) NOT NULL,
  `field_id` varchar(92) NOT NULL,
  `allow_multiple` bit(1) DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `display_name` varchar(45) DEFAULT NULL,
  `editable_condition` text DEFAULT NULL,
  `name` varchar(45) DEFAULT NULL,
  `parent_id` varchar(255) DEFAULT NULL,
  `schema_id` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `visibility_condition` text DEFAULT NULL,
  `default_lat` double DEFAULT NULL,
  `default_lon` double DEFAULT NULL,
  `default_date` datetime DEFAULT NULL,
  `default_selection` varchar(255) DEFAULT NULL,
  `options_data` text DEFAULT NULL,
  `default_boolean` bit(1) DEFAULT NULL,
  `default_string` varchar(255) DEFAULT NULL,
  `match_pattern` varchar(255) DEFAULT NULL,
  `max_length` int(11) DEFAULT NULL,
  `default_number` double DEFAULT NULL,
  `max_value` double DEFAULT NULL,
  `min_value` double DEFAULT NULL,
  PRIMARY KEY (`field_id`),
  KEY `idx_schema_id` (`schema_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `group_users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `group_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `group_id` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `user_id` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_id_user_id` (`group_id`,`user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `related_tickets`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `related_tickets` (
  `related_id` varchar(62) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `related_to_ticket_id` varchar(30) DEFAULT NULL,
  `relationship` varchar(45) DEFAULT NULL,
  `ticket_id` varchar(30) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`related_id`),
  KEY `idx_ticket_relation` (`ticket_id`,`relationship`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_contexts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_contexts` (
  `report_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `report_data` longtext DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_runs`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_runs` (
  `run_id` varchar(255) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `current_state` varchar(45) DEFAULT NULL,
  `deleted` bit(1) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `report_id` varchar(45) DEFAULT NULL,
  `run_date` datetime DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`run_id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_run_date` (`run_date`),
  KEY `idx_current_state` (`current_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reports`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reports` (
  `report_id` varchar(45) NOT NULL,
  `cql_query` varchar(4096) DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `cron` varchar(45) NOT NULL,
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(45) DEFAULT NULL,
  `provisioned_by` varchar(30) DEFAULT NULL,
  `recipients` varchar(2048) DEFAULT NULL,
  `scope_reference_id` varchar(255) DEFAULT NULL,
  `scope_type` varchar(45) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `roles` (
  `role_id` varchar(30) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(30) DEFAULT NULL,
  `permissions` text DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schema_summaries`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schema_summaries` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `created_by` varchar(30) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(45) NOT NULL,
  `schema_id` varchar(45) NOT NULL,
  `state` varchar(45) DEFAULT NULL,
  `state_changed_by` varchar(30) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schema_id` (`schema_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `skill_definitions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `skill_definitions` (
  `skill_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `name` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `skill_values`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `skill_values` (
  `value_id` varchar(92) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `skill_id` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `skill_value` varchar(45) NOT NULL,
  PRIMARY KEY (`value_id`),
  KEY `idx_skill_id` (`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subject_ids`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subject_ids` (
  `external_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `is_primary` bit(1) DEFAULT NULL,
  `sub_type` varchar(45) DEFAULT NULL,
  `subject_global_id` varchar(45) NOT NULL,
  `id_type` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `id_value` varchar(45) NOT NULL,
  `verification_status` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`external_id`),
  KEY `idx_sub_id` (`id_type`,`id_value`),
  KEY `idx_sub_subtype_id` (`id_type`,`sub_type`,`id_value`),
  KEY `idx_sub_global` (`subject_global_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subject_summaries`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subject_summaries` (
  `global_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `dob` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `gender` varchar(45) DEFAULT NULL,
  `name` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`global_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tasks`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tasks` (
  `task_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `execution_interval_ms` bigint(20) DEFAULT NULL,
  `last_execution_time` datetime DEFAULT NULL,
  `last_run_status` int(11) DEFAULT NULL,
  `name` varchar(45) DEFAULT NULL,
  `scope_reference_id` varchar(255) DEFAULT NULL,
  `scope_type` varchar(45) DEFAULT NULL,
  `task_data` text DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `task_meta` text DEFAULT NULL,
  `type` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`task_id`),
  KEY `idx_scope_reference_id` (`scope_reference_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ticket_attachments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ticket_attachments` (
  `attachment_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `creator` varchar(30) DEFAULT NULL,
  `deleted` bit(1) DEFAULT NULL,
  `encrypted` bit(1) DEFAULT NULL,
  `media_type` varchar(45) DEFAULT NULL,
  `size_in_bytes` bigint(20) DEFAULT NULL,
  `ticket_id` varchar(30) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `url` varchar(1027) DEFAULT NULL,
  PRIMARY KEY (`attachment_id`),
  KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ticket_comments`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ticket_comments` (
  `comment_id` varchar(45) NOT NULL,
  `author` varchar(30) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `reply_to_id` varchar(30) DEFAULT NULL,
  `ticket_id` varchar(30) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`comment_id`),
  KEY `idx_ticket_id_reply_to_id` (`ticket_id`,`reply_to_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ticket_field_values`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ticket_field_values` (
  `field_value_id` varchar(171) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `schema_field_id` varchar(139) DEFAULT NULL,
  `boolean_value` bit(1) DEFAULT NULL,
  `choices_value` varchar(512) DEFAULT NULL,
  `date_value` datetime DEFAULT NULL,
  `location_lat_value` double DEFAULT NULL,
  `location_lon_value` double DEFAULT NULL,
  `number_value` double DEFAULT NULL,
  `string_value` varchar(512) DEFAULT NULL,
  `field_type` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `ticket_id` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`field_value_id`),
  UNIQUE KEY `uk_ticket_field` (`ticket_id`,`schema_field_id`),
  KEY `idx_string_value` (`string_value`),
  KEY `idx_boolean_value` (`boolean_value`),
  KEY `idx_number_value` (`number_value`),
  KEY `idx_location_lat_value` (`location_lat_value`),
  KEY `idx_location_lon_value` (`location_lon_value`),
  KEY `idx_choices_value` (`choices_value`),
  KEY `idx_date_value` (`date_value`),
  CONSTRAINT `FK9n5dm6e4v1v6po1ybn3rwt2cf` FOREIGN KEY (`ticket_id`) REFERENCES `ticket_skeletons` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ticket_skeletons`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ticket_skeletons` (
  `ticket_id` varchar(30) NOT NULL,
  `assigned_to_group_id` varchar(45) DEFAULT NULL,
  `assigned_to_user_id` varchar(30) DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `created_by_user_id` varchar(30) DEFAULT NULL,
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `ext_ref_id` varchar(127) DEFAULT NULL,
  `ext_ref_source` varchar(127) DEFAULT NULL,
  `priority` varchar(45) DEFAULT NULL,
  `subject_id` varchar(45) DEFAULT NULL,
  `ticket_state_id` varchar(92) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `workflow_id` varchar(45) NOT NULL,
  PRIMARY KEY (`ticket_id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_created_by_user_id` (`created_by_user_id`),
  KEY `idx_assigned_to_group_id` (`assigned_to_group_id`),
  KEY `idx_assigned_to_user_id` (`assigned_to_user_id`),
  KEY `idx_subject_id` (`subject_id`),
  KEY `idx_ext_ref` (`ext_ref_source`,`ext_ref_id`),
  KEY `idx_ticket_state_id` (`ticket_state_id`),
  KEY `idx_priority` (`priority`),
  KEY `idx_created` (`created`),
  KEY `idx_updated` (`updated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_activation_links`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_activation_links` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `partitionId` int(11) NOT NULL,
  `state` varchar(45) NOT NULL,
  `token` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `user_id` varchar(30) NOT NULL,
  `valid_till` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_groups`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_groups` (
  `group_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `name` varchar(45) NOT NULL,
  `required_skills` text DEFAULT NULL,
  `type` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_passwords`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_passwords` (
  `user_id` varchar(30) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `failed_password_attempt` int(11) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_role_mappings`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_role_mappings` (
  `mapping_id` varchar(62) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `role_id` varchar(30) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `user_id` varchar(30) NOT NULL,
  PRIMARY KEY (`mapping_id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_sessions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_sessions` (
  `session_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `expiry` datetime DEFAULT NULL,
  `last_active` datetime DEFAULT NULL,
  `partition_id` int(11) NOT NULL,
  `state` varchar(45) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `user_id` varchar(30) NOT NULL,
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_user_id_session_id` (`user_id`,`session_id`,`partition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_skill_associations`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_skill_associations` (
  `association_id` varchar(170) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `skill_id` varchar(45) NOT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `user_id` varchar(30) NOT NULL,
  `value_id` varchar(92) NOT NULL,
  PRIMARY KEY (`association_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `user_id` varchar(30) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `email` varchar(30) NOT NULL,
  `name` varchar(127) NOT NULL,
  `state` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `user_type` int(11) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `webhook_action_header_templates`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `webhook_action_header_templates` (
  `id` varchar(255) NOT NULL,
  `active` bit(1) DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `name` varchar(45) DEFAULT NULL,
  `template` text DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `action_id` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_action_id_name` (`action_id`,`name`),
  CONSTRAINT `FKhbh0847quxhr8clj13u7xfy2k` FOREIGN KEY (`action_id`) REFERENCES `actions` (`action_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_selection_rules`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `workflow_selection_rules` (
  `rule_id` varchar(45) NOT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `rule` text DEFAULT NULL,
  `rule_type` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `workflow_id` varchar(92) DEFAULT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_state_transitions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `workflow_state_transitions` (
  `transition_id` varchar(254) NOT NULL,
  `action_id` text DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `from_state` varchar(92) DEFAULT NULL,
  `rule` text DEFAULT NULL,
  `to_state` varchar(92) DEFAULT NULL,
  `type` varchar(45) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `workflow_id` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`transition_id`),
  KEY `idx_workflow` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflow_states`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `workflow_states` (
  `state_id` varchar(92) NOT NULL,
  `allowed_actions` blob DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `display_name` varchar(45) DEFAULT NULL,
  `editable_fields` blob DEFAULT NULL,
  `required_fields` blob DEFAULT NULL,
  `terminal` bit(1) DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  `visible_actions` blob DEFAULT NULL,
  `visible_fields` blob DEFAULT NULL,
  `workflow_id` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`state_id`),
  KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `workflows`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `workflows` (
  `workflow_id` varchar(45) NOT NULL,
  `available_actions` text DEFAULT NULL,
  `created` datetime(3) DEFAULT current_timestamp(3),
  `deleted` bit(1) DEFAULT NULL,
  `description` varchar(512) DEFAULT NULL,
  `description_template` text DEFAULT NULL,
  `display_name` varchar(45) DEFAULT NULL,
  `schema_id` varchar(45) DEFAULT NULL,
  `start_state_id` varchar(92) DEFAULT NULL,
  `state` varchar(45) DEFAULT NULL,
  `subject_id_template` text DEFAULT NULL,
  `title_template` text DEFAULT NULL,
  `updated` datetime(3) DEFAULT current_timestamp(3) ON UPDATE current_timestamp(3),
  PRIMARY KEY (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-03-06 14:58:20
