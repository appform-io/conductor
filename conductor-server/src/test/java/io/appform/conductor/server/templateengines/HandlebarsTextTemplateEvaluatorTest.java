package io.appform.conductor.server.templateengines;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.base.Strings;
import io.appform.conductor.model.workflow.Template;
import io.dropwizard.jackson.Jackson;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class HandlebarsTextTemplateEvaluatorTest {

    private ObjectMapper mapper = Jackson.newObjectMapper();
    private HandlebarsTextTemplateEvaluator handlebarTextTemplateEvaluator =
            new HandlebarsTextTemplateEvaluator(mapper);


    @Test
    public void testHandlebarTextTemplateEvaluatorSuccess() throws Exception {
        val payload = mapper.readTree("""
                {
                  "subject" : {
                     "number" : "12345"
                     }
                }
                """);
        val template = new Template(Template.Type.HANDLEBARS, "{{subject.number}}");
        val extractedSubject = handlebarTextTemplateEvaluator.evaluate(template, payload);
        assertTrue(extractedSubject.isPresent());
        assertEquals("12345", extractedSubject.get());
    }

    @Test
    @SneakyThrows
    public void testParseToInt() {
        assertEquals("1", handlebarTextTemplateEvaluator.evaluate( new Template(Template.Type.HANDLEBARS,"{{toInt value}}"),
                mapper.createObjectNode().put("value", "1")).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate( new Template(Template.Type.HANDLEBARS,"{{toInt value}}"),
                mapper.createObjectNode().put("value", "")).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate( new Template(Template.Type.HANDLEBARS,"{{toInt value}}"),
                mapper.createObjectNode().put("value", "abc")).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate( new Template(Template.Type.HANDLEBARS,"{{toInt value}}"),
                mapper.createObjectNode()).get());

    }

    @Test
    @SneakyThrows
    public void mapLookup() {
        val template =  new Template(Template.Type.HANDLEBARS,"{\"language\" : {{{ mapLookup op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]/0'}}} }");
        val inputNode = mapper.createObjectNode().set("payload[question1]", mapper.createArrayNode().add("2"));
        assertEquals("KA",
                mapper.readTree(handlebarTextTemplateEvaluator.evaluate(template, inputNode).get())
                        .get("language")
                        .asText());
    }

    @Test
    @SneakyThrows
    public void transformBool() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookup op_1=true op_2=false pointer='/payload[question2]/0'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode language = objectMapper.readTree(
                        handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .set("payload[question2]",
                                                objectMapper.createArrayNode().add("1"))).get())
                .get("language");
        assertTrue(language.asBoolean());
    }

    @Test
    @SneakyThrows
    public void transformArray() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .set("payload[question1]",
                                                objectMapper.createArrayNode()
                                                        .add("1")
                                                        .add("2"))).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(2, jsonNode.size());
        assertEquals("EN", jsonNode.get(0).asText());
        assertEquals("KA", jsonNode.get(1).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayClubbed() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .set("payload[question1]",
                                                objectMapper.createArrayNode()
                                                        .add("19")
                                                        .add("2"))).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(3, jsonNode.size());
        assertEquals("EN", jsonNode.get(0).asText());
        assertEquals("HI", jsonNode.get(1).asText()); //( gets replaced by last value
        assertEquals("KA", jsonNode.get(2).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayIntConversion() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .set("payload[question1]",
                                                objectMapper.createArrayNode()
                                                        .add(19)
                                                        .add(2))).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(3, jsonNode.size());
        assertEquals("EN", jsonNode.get(0).asText());
        assertEquals("HI", jsonNode.get(1).asText()); //( gets replaced by last value
        assertEquals("KA", jsonNode.get(2).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayInvalidTextValueToDefault() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .set("payload[question1]",
                                                objectMapper.createArrayNode()
                                                        .add("a")
                                                        .add("122"))).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(3, jsonNode.size());
        assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
        assertEquals("EN", jsonNode.get(1).asText());
        assertEquals("KA", jsonNode.get(2).asText());
    }

    @Test
    @SneakyThrows
    public void transformArrayInvalidTypeToDefault() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .set("payload[question1]",
                                                objectMapper.createArrayNode()
                                                        .add(true)
                                                        .add(false)
                                                        .add(true)
                                                        .add(true))).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(1, jsonNode.size());
        assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
    }

    @Test
    @SneakyThrows
    public void transformArrayMissingNodeToDefault() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(1, jsonNode.size());
        assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
    }

    @Test
    @SneakyThrows
    public void transformArrayEmptyKeyToDefault() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer=''}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(1, jsonNode.size());
        assertEquals("HI", jsonNode.get(0).asText()); //( gets replaced by last value
    }

    @Test
    @SneakyThrows
    public void transformArraySingleElement() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .put("payload[question1]", "2")).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(1, jsonNode.size());
        assertEquals("KA", jsonNode.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void transformArraySingleElementDefaultSelect() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ mapLookupArr op_1='EN' op_2='KA' op_3='HI' pointer='/payload[question1]'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode().put("payload[question1]", "")).get())
                .get("language");
        assertTrue(jsonNode.isArray());
        assertEquals(1, jsonNode.size());
        assertEquals("HI", jsonNode.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void translate() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ translate op_telegu='TG' op_tamil='TM' op_hindi='HI' op_english='EN' pointer='/ticket.cf_language'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        assertEquals("HI",
                objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                        objectMapper.createObjectNode()
                                                .put("ticket.cf_language", "Hindi")).get())
                        .get("language")
                        .asText());
    }

    @Test
    @SneakyThrows
    public void translateBool() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ translate op_true='Resolved' op_false='Closed' pointer='/ticket.cf_resolved'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        assertEquals("Resolved",
                objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                        objectMapper.createObjectNode()
                                                .put("ticket.cf_resolved", true)).get())
                        .get("language")
                        .asText());
    }

    @Test
    @SneakyThrows
    public void testAdd() {

        val node = Jackson.newObjectMapper()
                .createObjectNode()
                .put("days", 2);
        assertEquals("7", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{add days 5}}"), node).get());
    }

    @Test
    @SneakyThrows
    public void testURLEncode() {

        val node = Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "Blah Blah");
        assertEquals("Blah+Blah", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{URLEncode name}}"), node).get());
    }

    @Test
    @SneakyThrows
    public void translateInt() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ translate op_1='Resolved' op_2='Closed' pointer='/ticket.cf_resolved'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        assertEquals("Resolved",
                objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                        objectMapper.createObjectNode()
                                                .put("ticket.cf_resolved", 1)).get())
                        .get("language")
                        .asText());
    }

    @Test
    @SneakyThrows
    public void translateMissing() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ translate op_1='Resolved' op_2='Closed' pointer='/ticket.cf_resolved'}}} }");
        assertEquals("null", mapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                mapper.createObjectNode()
                                        .put("ticket.cf_resolved", 9)).get())
                .get("language")
                .asText());
    }

    @Test
    @SneakyThrows
    public void translateArray() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ translateArr op_telegu='TG' op_tamil='TM' op_hindi='HI' op_english='EN' pointer='/ticket.cf_language'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode arr = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .set("ticket.cf_language",
                                                objectMapper.createArrayNode()
                                                        .add("Telegu")
                                                        .add("Hindi"))).get())
                .get("language");
        assertTrue(arr.isArray());
        assertEquals(2, arr.size());
        assertEquals("TG", arr.get(0).asText());
        assertEquals("HI", arr.get(1).asText());
    }

    @Test
    @SneakyThrows
    public void translateArraySingleElement() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ translateArr op_telegu='TG' op_tamil='TM' op_hindi='HI' op_english='EN' pointer='/ticket.cf_language'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode arr = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .put("ticket.cf_language", "Telegu")).get())
                .get("language");
        assertTrue(arr.isArray());
        assertEquals(1, arr.size());
        assertEquals("TG", arr.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void translateToHTML() {
        val template =  new Template(Template.Type.HANDLEBARS, "<b>Age:</b> {{{translateTxt op_true='Above Sixty' op_false='Below Sixty' pointer='/ageAboveSixty'}}}<br> <b>Travel Status:</b> {{{translateTxt op_true='Travelled abroad' op_false='Has not travelled abroad' pointer='/travelled'}}}<br><b>Exposure:</b> {{{translateTxt op_true='Has had contact with COVID-19 patient' op_false='No contact' pointer='/contact'}}}<br> <b>Pre existing conditions:</b> {{{translateTxt op_heart='Heart problems, Asthma or any other Lung problems' op_cancer='Cancer or on chemotherapy or other low immunity problems' op_diabetes='Diabetes or Kidney problems' op_pregnant='Pregnant at present or recently delivered a baby' op_none='No Prexistting Conditions' pointer='/existingDiseases'}}}<br> <b>Symptoms:</b> {{{translateArrTxt op_fever='Fever' op_cough='Dry Cough' op_throatpain='Throat Pain' op_wheezing='Wheezing' op_others='Others' op_none='none' pointer='/symptoms'}}}");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        assertEquals(
                "<b>Age:</b> Above Sixty<br> <b>Travel Status:</b> Travelled abroad<br><b>Exposure:</b> Has had contact with COVID-19 patient<br> <b>Pre existing conditions:</b> Diabetes or Kidney problems<br> <b>Symptoms:</b> Dry Cough, Throat Pain, Wheezing",
                handlebarTextTemplateEvaluator.evaluate(template,
                        objectMapper.readTree("{\n" +
                                "   \"ageAboveSixty\":true,\n" +
                                "   \"travelled\":true,\n" +
                                "   \"contact\":true,\n" +
                                "   \"existingDiseases\":\"diabetes\",\n" +
                                "   \"symptoms\":[\n" +
                                "      \"cough\",\n" +
                                "      \"throatpain\",\n" +
                                "      \"wheezing\"\n" +
                                "   ]\n" +
                                "}")).get());
    }
    @Test
    @SneakyThrows
    public void translateToHTMLNull() {
        val template =  new Template(Template.Type.HANDLEBARS, "<b>Age:</b> Above Sixty<br> <b>Travel Status:</b> <br><b>Exposure:</b> Has had contact with COVID-19 patient<br> <b>Pre existing conditions:</b> Diabetes or Kidney problems<br> <b>Symptoms:</b> Dry Cough, Throat Pain, Wheezing");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        assertEquals(
                "<b>Age:</b> Above Sixty<br> <b>Travel Status:</b> <br><b>Exposure:</b> Has had contact with COVID-19 patient<br> <b>Pre existing conditions:</b> Diabetes or Kidney problems<br> <b>Symptoms:</b> Dry Cough, Throat Pain, Wheezing",
                handlebarTextTemplateEvaluator.evaluate(template,
                        objectMapper.readTree("{\n" +
                                "   \"ageAboveSixty\":true,\n" +
                                "   \"travelled\":93,\n" +
                                "   \"contact\":true,\n" +
                                "   \"existingDiseases\":\"diabetes\",\n" +
                                "   \"symptoms\":[\n" +
                                "      \"cough\",\n" +
                                "      \"throatpain\",\n" +
                                "      \"blah\",\n" +
                                "      \"wheezing\"\n" +
                                "   ]\n" +
                                "}")).get());
    }

    @Test
    @SneakyThrows
    public void translateSpaceWords() {
        val template =  new Template(Template.Type.HANDLEBARS, "{\"language\" : {{{ translateArr op_hello_world='HW' op_hello_mars='HM' pointer='/ticket.cf_language'}}} }");
        final ObjectMapper objectMapper = Jackson.newObjectMapper();
        final JsonNode arr = objectMapper.readTree(
                         handlebarTextTemplateEvaluator.evaluate(template,
                                objectMapper.createObjectNode()
                                        .put("ticket.cf_language", "Hello World")).get())
                .get("language");
        assertTrue(arr.isArray());
        assertEquals(1, arr.size());
        assertEquals("HW", arr.get(0).asText());
    }

    @Test
    @SneakyThrows
    public void phoneTest() {
        val template =  new Template(Template.Type.HANDLEBARS, "{{phone value}}");
        assertEquals("1234567890", handlebarTextTemplateEvaluator.evaluate(
                template,  mapper.valueToTree(Collections.singletonMap("value", "1234567890sdasa%^%$$$^$ %$ _"))).get());
        assertEquals("1234567890",
                handlebarTextTemplateEvaluator.evaluate(template, mapper.valueToTree(Collections.singletonMap("value", "+91 1234567890"))).get());
        assertEquals("1234567890",
                handlebarTextTemplateEvaluator.evaluate(template, mapper.valueToTree(Collections.singletonMap("value", "01234567890"))).get());
        assertEquals("1234567890",
                handlebarTextTemplateEvaluator.evaluate(template, mapper.valueToTree(Collections.singletonMap("value", "+91-1234567890"))).get());
        assertEquals("1234567890",
                handlebarTextTemplateEvaluator.evaluate(template, mapper.valueToTree(Collections.singletonMap("value", "1234567890"))).get());
        assertEquals("",
                handlebarTextTemplateEvaluator.evaluate(template, mapper.valueToTree(Collections.emptyMap())).get());
        assertEquals("",
                handlebarTextTemplateEvaluator.evaluate(template, mapper.valueToTree(Collections.singletonMap("value", "1234"))).get());
        assertEquals("", handlebarTextTemplateEvaluator.evaluate(
                template, mapper.valueToTree(Collections.singletonMap("value", "sasdasd asdqqweq weq we"))).get());

    }

    @Test
    public void testNormalize() {

        val node = Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "Dr. Jeykll & Mr. Hyde")
                .put("state", "punjab");
        assertEquals("dr_jeykll_mr_hyde", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{normalize name}}"), node).get());
        assertEquals("DR_JEYKLL_MR_HYDE", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{normalizeUpper name}}"), node).get());
        assertEquals("Punjab", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{normalizeInitCap state}}"), node).get());
    }

    @Test
    public void testAlphaNumeric() {

        val node = Jackson.newObjectMapper()
                .createObjectNode()
                .put("name", "Dr. Jeykll \\ Mr. Hyde")
                .put("state", "punjab");
        assertEquals("Dr Jeykll Mr Hyde", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{alphaNumeric name}}"), node).get());
    }



    @Test
    public void testSingleLine() throws Exception {
        val template = new Template(Template.Type.HANDLEBARS,  "{\n" +
                "  \"provider\": \"freshdesk\",\n" +
                "  \"providerTicketId\": \"{{body.freshdesk_webhook.ticket_id}}\",\n" +
                "  \"providerTicketUrl\": \"{{body.freshdesk_webhook.ticket_url}}\",\n" +
                "  \"providerTicketTags\": \"{{body.freshdesk_webhook.ticket_tags}}\",\n" +
                "  \"providerTicketGroupName\": \"{{normalize body.freshdesk_webhook.ticket_group_name}}\",\n" +
                "  \"providerTicketAgentName\": \"{{normalize body.freshdesk_webhook.ticket_agent_name}}\",\n" +
                "  \"providerTicketAgentEmail\": \"{{body.freshdesk_webhook.ticket_agent_email}}\",\n" +
                "  \"providerTicketStatus\": \"{{normalize body.freshdesk_webhook.ticket_status}}\",\n" +
                "  \"providerTicketForeignTravelHistory\": \"{{normalize body.freshdesk_webhook.ticket_cf_foreign_travel_history}}\",\n" +
                "  \"providerTicketPatientName\": \"{{body.freshdesk_webhook.ticket_cf_patient_name}}\",\n" +
                "  \"providerTicketPincode\": \"{{body.freshdesk_webhook.ticket_cf_pin_code}}\",\n" +
                "  \"providerTicketState\": \"{{normalize body.freshdesk_webhook.ticket_cf_state}}\",\n" +
                "  \"providerTicketContact\": \"{{normalize body.freshdesk_webhook.ticket_cf_contact}}\",\n" +
                "  \"providerTicketType\": \"{{normalize body.freshdesk_webhook.ticket_ticket_type}}\",\n" +
                "  \"providerTicketFsmPhoneNumber\": \"{{body.freshdesk_webhook.ticket_cf_fsm_phone_number}}\",\n" +
                "  \"providerTicketDoctorNotes\": \"{{singleLineText body.freshdesk_webhook.ticket_cf_doctor_notes}}\",\n" +
                "  \"providerTicketRecommendation\": \"{{normalize body.freshdesk_webhook.ticket_cf_category}}\",\n" +
                "  \"providerTicketFsmServiceLocation\": \"{{body.freshdesk_webhook.ticket_cf_fsm_service_location}}\",\n" +
                "  \"providerTicketFsmAppointmentEndTime\": \"{{body.freshdesk_webhook.ticket_cf_fsm_appointment_end_time}}\",\n" +
                "  \"providerTicketPatientLanguage\": \"{{normalize body.freshdesk_webhook.ticket_cf_patient_language}}\",\n" +
                "  \"providerTicketFsmAppointmentStartTime\": \"{{body.freshdesk_webhook.ticket_cf_fsm_appointment_start_time}}\",\n" +
                "  \"providerTicketPatientAge\": \"{{body.freshdesk_webhook.ticket_cf_patient_age}}\",\n" +
                "  \"providerTicketPatientGender\": \"{{normalize body.freshdesk_webhook.ticket_cf_patient_gender}}\"\n" +
                "}");

        val payload = "{\n" +
                "  \"apiPath\": \"/callbacks/FRESHDESK\",\n" +
                "  \"body\": {\n" +
                "    \"freshdesk_webhook\": {\n" +
                "      \"ticket_cf_patient_name\": \"Name\",\n" +
                "      \"ticket_ticket_type\": \"Non - Covid Home Lockdown\",\n" +
                "      \"ticket_cf_fsm_contact_name\": null,\n" +
                "      \"ticket_cf_fsm_service_location\": \"bp\",\n" +
                "      \"ticket_cf_patient_language\": \"Hindi\",\n" +
                "      \"ticket_cf_fsm_phone_number\": null,\n" +
                "      \"ticket_group_name\": \"32423\",\n" +
                "      \"ticket_id\": 111,\n" +
                "      \"ticket_cf_fsm_appointment_end_time\": null,\n" +
                "      \"ticket_cf_fsm_appointment_start_time\": null,\n" +
                "      \"ticket_url\": \"\",\n" +
                "      \"ticket_cf_foreign_travel_history\": \"No\",\n" +
                "      \"ticket_cf_patient_gender\": \"Other\",\n" +
                "      \"ticket_tags\": \"\",\n" +
                "      \"ticket_cf_fsm_customer_signature\": \"233232423\",\n" +
                "      \"ticket_cf_doctor_notes\": \"fatigue since 2 days.\\n mild sore throat.  mild headache. \\nNo other symptoms \\nRED FLAGS EXPLAINED \\nHome isolation Adviced.\",\n" +
                "      \"ticket_cf_patient_age\": 45,\n" +
                "      \"ticket_status\": \"resolved\",\n" +
                "      \"ticket_agent_name\": \"\",\n" +
                "      \"ticket_cf_pin_code\": null,\n" +
                "      \"ticket_cf_state\": null,\n" +
                "      \"ticket_agent_email\": \"\",\n" +
                "      \"ticket_cf_category\": null,\n" +
                "      \"ticket_cf_contact\": null\n" +
                "    }\n" +
                "  },\n" +
                "  \"id\": \"FRESHDESK\"\n" +
                "}";
        val mapper = Jackson.newObjectMapper();

        val node = mapper.readTree(payload);
        val translatedPayload = mapper.readTree(handlebarTextTemplateEvaluator.evaluate(template, node).get());
        assertTrue(translatedPayload.get("providerTicketDoctorNotes").asText().equals("fatigue since 2 days. mild sore throat. mild headache. No other symptoms RED FLAGS EXPLAINED Home isolation Adviced."));

    }

    @Test
    public void testElapsedTime() {

        val currDate = new Date();
        assertTrue(currDate.getTime() <= Long.parseLong(
                Objects.requireNonNull(handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{currTime}}"),
                        Jackson.newObjectMapper().createObjectNode()).get())));
    }


    @Test
    @SneakyThrows
    public void testElapsedTime1() {

        val currDate = new Date();
        val node = Jackson.newObjectMapper()
                .readTree("{\n" +
                        "   \"StartTime\" : [\n" +
                        "      \"2020-04-12 23:25:15\"\n" +
                        "   ],\n" +
                        "   \"CurrentTime\" : [\n" +
                        "      \"2020-04-12 23:25:59\"\n" +
                        "   ]\n" +
                        "}\n");
        final String out = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{elapsedTime \"YYYY-MM-dd HH:mm:ss\" StartTime/0 CurrentTime/0}}"), node).get();
        assertEquals("44000", out);
    }

    @Test
    @SneakyThrows
    public void testEmpty() {

        val currDate = new Date();
        final ObjectMapper mapper = Jackson.newObjectMapper();
        val node = mapper
                .createObjectNode();
        node.set("arr", mapper.createArrayNode());
        node.set("nodata", mapper.createArrayNode().add(""));
        node.set("data", mapper.createArrayNode().add("1").add("2"));

        assertEquals("true", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{emptyNode arr}}"), node).get());
        assertEquals("false", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{notEmptyNode arr}}"), node).get());

        assertEquals("true", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{emptyNode nodata}}"), node).get());
        assertEquals("false", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{notEmptyNode nodata}}"), node).get());

        assertEquals("false", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{emptyNode data}}"), node).get());
        assertEquals("true", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{notEmptyNode data}}"), node).get());
    }



    @Test
    @SneakyThrows
    public void testParseToIntWeird() {

        final ObjectMapper mapper = Jackson.newObjectMapper();


        assertEquals("1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{toIntPtr pointer='/value[xx]/0'}}"),
                mapper.createObjectNode().set("value[xx]", mapper.createArrayNode().add("1"))).get());


    }

    @Test
    @SneakyThrows
    public void testTrimDecimalPoints() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPoints value}}"), mapper.createObjectNode()).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPoints value}}"), mapper.createObjectNode().set("value", null)).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPoints value}}"), mapper.createObjectNode().put("value", "")).get());
        assertEquals("", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPoints value}}"), mapper.createObjectNode().put("value", ".0123")).get());
        assertEquals("0", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPoints value}}"), mapper.createObjectNode().put("value", "0.0123")).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPoints value}}"), mapper.createObjectNode().put("value", "9988771212.0")).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPoints value}}"), mapper.createObjectNode().put("value", "9988771212")).get());
    }

    @Test
    @SneakyThrows
    public void testTrimDecimalPointsPtr() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), null).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), NullNode.getInstance()).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), MissingNode.getInstance()).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("123123"))).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer=''}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("123123"))).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), mapper.createObjectNode().set("value", null)).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add(NullNode.getInstance()))).get());
        assertEquals("-1", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add(""))).get());
        assertEquals("", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add(".0123"))).get());
        assertEquals("0", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("0.0123"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("9988771212.0"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{trimDecimalPointsPtr pointer='/value/0'}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("9988771212"))).get());
    }
    @Test
    @SneakyThrows
    public void testTrimDecimalPointsPtrWithPhone() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("9988771212.0"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("9988771212"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("+91-9988771212.0"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("+91-9988771212"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("09988771212.0"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("09988771212"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("919988771212.0"))).get());
        assertEquals("9988771212", handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{phone (trimDecimalPointsPtr pointer='/value/0')}}"), mapper.createObjectNode().set("value", mapper.createArrayNode().add("919988771212"))).get());
    }

    @Test
    @SneakyThrows
    public void testParseHTML2Text() {

        final ObjectMapper mapper = Jackson.newObjectMapper();


        final String value = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{html2Text value}}"),
                mapper.createObjectNode()
                        .put("value",
                                "<div style=\\\"font-family:-apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif; font-size:14px\\\">\\n<div> </div>\\n<table border=\\\"0\\\" cellpadding=\\\"0\\\" cellspacing=\\\"0\\\" style=\\\"border-collapse:collapse;width:986.27pt\\\" width=\\\"1315\\\"><tbody><tr><td class=\\\"et2\\\" style=\\\"color:#000000;font-size:15px;font-weight:400;font-style:normal;text-decoration:none;font-family:Calibri;border:none;text-align:general;vertical-align:bottom;height:14.25pt;width:986.55pt\\\" width=\\\"100%\\\">Citizen called to request for medicines. Have informed them to call number 104 and press 2 </td></tr></tbody></table>\\n</div>")).get();
        assertEquals("Citizen called to request for medicines. Have informed them to call number 104 and press 2", value);
    }

    @Test
    @SneakyThrows
    public void testLocalTime() {

        final ObjectMapper mapper = Jackson.newObjectMapper();


        final String value = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{{localTime 'Asia/Kolkata'}}}"),
                mapper.createObjectNode()).get();
        assertFalse(Strings.isNullOrEmpty(value));
    }

    @Test
    @SneakyThrows
    public void testMapArrLookup() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "karnataka");
            obj.put("language", 1);
            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup array='english,hindi,tamil,bengali,kannada' op_karnataka='2,3,1' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("hindi", res);
        }



        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "karnataka");
            obj.put("language", 6);
            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup array='english,hindi,tamil,bengali,kannada' op_karnataka='2,3,1' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("english", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();

            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup array='english,hindi,tamil,bengali,kannada' op_karnataka='2,3,1' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("english", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "wrongstate");
            obj.put("language", 1);

            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup array='english,hindi,tamil,bengali,kannada' op_karnataka='2,3,1' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("english", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "karnataka");
            obj.put("language", 1);
            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup array='english,hindi,tamil,bengali,kannada' op_karnataka='2,3,1' op_tamilnadu='3,2,1' }}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("english", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "karnataka");
            obj.put("language", 1);
            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup array='english,hindi,tamil,bengali,kannada' op_karnataka='20,30,11' op_tamilnadu='30,24,11' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("english", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "karnataka");
            obj.put("language", 1);
            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup op_karnataka='20,30,11' op_tamilnadu='30,24,11' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "karnataka");
            obj.put("language", 1);
            val template = new Template(Template.Type.HANDLEBARS,"{{mapArrLookup}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            assertEquals("", res);
        }
    }

    @Test
    public void testCountIf() {
        val node = mapper.createObjectNode()
                .put("question1", 1)
                .put("question2", 2)
                .put("question3", 1);

        final String value = handlebarTextTemplateEvaluator.evaluate(
                new Template(Template.Type.HANDLEBARS,"{{{countIf op_1=true op_2=false pointers='/question1,/question2,/question3,/question4'}}}"),
                node).get();
        assertEquals("2", value);
    }

    @Test
    public void testStrMatch() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        val node = mapper.createObjectNode()
                .put("question1", "blah1")
                .put("question2", "blah2")
                .put("question3", "blah3");

        final String value = handlebarTextTemplateEvaluator.evaluate(
                new Template(Template.Type.HANDLEBARS,"{{{countMatchStr op_blah1=true op_blah2=true pointer='/question1,/question2,/question3,/question4'}}}"),
                node).get();
        assertEquals("2", value);
    }

    @Test
    public void testStrEq() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        val node = mapper.createObjectNode()
                .put("question1", "1 1");

        final String value = handlebarTextTemplateEvaluator.evaluate(
                new Template(Template.Type.HANDLEBARS,"{{#if (streq question1 \"1 1\")}}greaterThanTwo{{/if}}"),
                node).get();
        assertEquals("greaterThanTwo", value);
    }

    @Test
    @SneakyThrows
    public void testMultiMapLookup() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "Karnataka");
            obj.put("language", 1);
            val template = new Template(Template.Type.HANDLEBARS,"{{multiMapLookup default='english' op_karnataka='1:kannada,2:hindi,3:english,5:tamil' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            System.out.println("case-1:" + res);
            assertEquals("kannada", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "Karnataka");
            obj.put("language", 100);
            val template = new Template(Template.Type.HANDLEBARS,"{{multiMapLookup default='english' op_karnataka='1:kannada,2:hindi,3:english,5:tamil' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            System.out.println("case-2:" + res);
            assertEquals("english", res);
        }
        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "nonexistent");
            obj.put("language", 100);
            val template = new Template(Template.Type.HANDLEBARS,"{{multiMapLookup default='english' op_karnataka='1:kannada,2:hindi,3:english,5:tamil' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            System.out.println("case-3:" + res);
            assertEquals("english", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "Karnataka");
            obj.put("language", 200);
            val template = new Template(Template.Type.HANDLEBARS,"{{multiMapLookup default='english' op_karnataka='100:kannada,200:hindi,300:english,5:tamil' op_tamilnadu='3,2,1' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            System.out.println("case-4:" + res);
            assertEquals("hindi", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "Karnataka");
            obj.put("language", 200);
            val template = new Template(Template.Type.HANDLEBARS,"{{multiMapLookup default='english' key1='/state' key2='/language'}}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            System.out.println("case-5:" + res);
            assertEquals("english", res);
        }

        {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("state", "Karnataka");
            obj.put("language", 200);
            val template = new Template(Template.Type.HANDLEBARS,"{{multiMapLookup default='english' }}");
            String res = handlebarTextTemplateEvaluator.evaluate(template, obj).get();
            System.out.println("case-5:" + res);
            assertEquals("english", res);
        }


    }

    @Test
    public void testNumWithinRange() {

        final ObjectMapper mapper = Jackson.newObjectMapper();

        val node = mapper.createObjectNode()
                .put("question1", 5)
                .put("question3", 1);


        {
            final String value = handlebarTextTemplateEvaluator.evaluate(
                    new Template(Template.Type.HANDLEBARS,"{{selectValuesGivenRange question1 2 100 3 2}}"),
                    node).get();
            assertEquals("3", value);
        }

        {
            final String value = handlebarTextTemplateEvaluator.evaluate(
                    new Template(Template.Type.HANDLEBARS,"{{selectValuesGivenRange question1 20 100 'a' 'b'}}"),
                    node).get();
            assertEquals("b", value);
        }

        {
            final String value = handlebarTextTemplateEvaluator.evaluate(
                    new Template(Template.Type.HANDLEBARS,"{{selectValuesGivenRange question3 20 100 'a' 'b'}}"),
                    node).get();
            assertEquals("b", value);
        }
    }

    @Test
    public void testDateFormat() {


        final ObjectMapper mapper = Jackson.newObjectMapper();

        val node = mapper.createObjectNode()
                .set("dataObject", mapper.createObjectNode()
                        .set("data", mapper.createObjectNode()
                                .set("endTime", new LongNode(1618770600000L))));

        final String transformed = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{dateFormat dataObject.data.endTime 'MM/dd/yyyy'}}"), node).get();

        assertEquals("04/19/2021", transformed);
    }

    @Test
    public void testToEpochAndAddDays() {

        val node = mapper.createObjectNode()
                .set("date", new TextNode("15 Apr, 2021"));

        final String transformed = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{add 864000000 (toEpochTime date 'dd MMM, yyyy')}}"), node).get();

        assertEquals("1619289000000", transformed);
    }

    @Test
    public void testIsDigits() {


        val mapper = Jackson.newObjectMapper();

        val numberTransformed = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{#if (isDigits body)}}{{body}}{{/if}}"),
                        mapper.createObjectNode().set("body", new TextNode("10"))).get();
        assertEquals("10", numberTransformed);

        val decimalTransformed = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{#if (isDigits body)}}{{body}}{{/if}}"),
                        mapper.createObjectNode().set("body", new TextNode("10.0"))).get();
        assertEquals("", decimalTransformed);

        val negativeTransformed = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{#if (isDigits body)}}{{body}}{{/if}}"),
                        mapper.createObjectNode().set("body", new TextNode("-10"))).get();
        assertEquals("", negativeTransformed);

        val textTransformed = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{#if (isDigits body)}}{{body}}{{/if}}"),
                        mapper.createObjectNode().set("body", new TextNode("Hi"))).get();
        assertEquals("", textTransformed);
    }

    @Test
    public void testValueFromJsonString() {


        val mapper = Jackson.newObjectMapper();

        val node = mapper.createObjectNode()
                .set("message", mapper.createArrayNode().add(TextNode.valueOf(
                        "[{\"from\":\"919988776655\",\"id\":\"893649f5-9cf9-4422-87d1-a9c4258045e7\",\"timestamp\":\"1620140858\",\"type\":\"text\",\"text\":{\"body\":\"Test\"},\"profile\":{\"name\":\"Tester User\"},\"wa_number\":\"919745697456\"}]")));

        val transformed = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{valueFromJsonString message/0 pointer='/0/id'}}"), node).get();
        assertEquals("893649f5-9cf9-4422-87d1-a9c4258045e7", transformed);

        val incorrectPointer = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{valueFromJsonString message/0 pointer='/1/id'}}"), node).get();
        assertEquals("", incorrectPointer);
    }

    @Test
    public void testStringEqualsIgnoreCase() {


        val mapper = Jackson.newObjectMapper();

        val expectedMatch = "matched";
        val expectedMisMatch = "mismatch";

        val exactMatch = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{#if (streqi message 'covidhelp')}}matched{{else}}mismatch{{/if}}"),
                        mapper.createObjectNode().set("message", TextNode.valueOf("covidhelp"))).get();
        assertEquals(expectedMatch, exactMatch);

        val IgnoreCaseMatch = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{#if (streqi message 'covidhelp')}}matched{{else}}mismatch{{/if}}"),
                        mapper.createObjectNode().set("message", TextNode.valueOf("CovidHelp"))).get();
        assertEquals(expectedMatch, IgnoreCaseMatch);

        val noMatch = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{#if (streqi message 'covidhelp')}}matched{{else}}mismatch{{/if}}"),
                        mapper.createObjectNode().set("message", TextNode.valueOf("Help"))).get();
        assertEquals(expectedMisMatch, noMatch);
    }

    @Test
    @SneakyThrows
    public void testSanitizeJson() {


        val mapper = Jackson.newObjectMapper();

        val actualBody = "\n\t\"\\'& \r";

        val specialCharsTransformation = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS,"{ \"body\" : {{{sanitizeJson body/0}}} }"),
                mapper.createObjectNode().set("body",
                        mapper.createArrayNode().add(actualBody))).get();
        val expectedSpecialChars = "{ \"body\" : \"\\n\\t\\\"\\\\'& \\r\" }";
        assertEquals(expectedSpecialChars, specialCharsTransformation);

        // Mapper should be able to load transformed string value
        val transformedTree = mapper.readTree(specialCharsTransformation);
        val resultBody = transformedTree.get("body").asText();
        assertEquals(actualBody, resultBody);

        val nullBody = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS,"{ \"body\" : {{{sanitizeJson body/0}}} }"),
                mapper.createObjectNode().set("body",
                        mapper.createArrayNode().add(NullNode.getInstance()))).get();
        val expectedNullBody = "{ \"body\" : null }";
        assertEquals(expectedNullBody, nullBody);

        val emptyBody = handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS,"{ \"body\" : {{{sanitizeJson body/0}}} }"),
                mapper.createObjectNode().set("body",
                        mapper.createArrayNode().add(TextNode.valueOf("")))).get();
        val expectedEmptyBody = "{ \"body\" : \"\" }";
        assertEquals(expectedEmptyBody, emptyBody);
    }

    @Test
    public void testToComputeDays() {
        final ObjectMapper mapper = Jackson.newObjectMapper();

        val node = mapper.createObjectNode()
                .set("date", new TextNode("01 Jan, 2021"));

        final String days = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{computeDays date 'dd MMM, yyyy'}}"), node).get();

        assertTrue(Long.parseLong(days) > 1320);
    }

    @Test
    public void testToComputeDaysWhenDateIsAfterCurrentTime() {
        val node = mapper.createObjectNode()
                .set("date", new TextNode("01 Jan, 2021"));

        final String days = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{computeDays date 'dd MMM, yyyy' }}"), node).get();

        assertTrue(Long.parseLong(days) > 1320);
    }

    @Test
    public void testToComputeDaysWhenDateFormatIsAbsent() {
        final ObjectMapper mapper = Jackson.newObjectMapper();

        val node = mapper.createObjectNode()
                .set("date", new TextNode("01-01-2021"));

        final String days = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{computeDays date}}"), node).get();

        assertTrue( Long.parseLong(days) > 1320);
    }

    @Test
    public void testToComputeDaysWithTimeZone() {
        val node = mapper.createObjectNode()
                .set("date", new TextNode("01-01-2021"));

        final String days = handlebarTextTemplateEvaluator
                .evaluate(new Template(Template.Type.HANDLEBARS,"{{computeDays date 'dd-MM-yyy' 'Europe/London'}}"), node).get();

        assertTrue(Long.parseLong(days) > 1320);
    }

    @Test
    public void testToGetDateByN() {

        val sdf = new SimpleDateFormat("dd-MM-yyyy");
        final ObjectMapper mapper = Jackson.newObjectMapper();


        assertEquals(sdf.format(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)).getTime())
                , handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{incDate value}}"), mapper.createObjectNode().put("value", 1)).get());


        assertEquals(sdf.format(Date.from(Instant.now().plus(0, ChronoUnit.DAYS)).getTime())
                , handlebarTextTemplateEvaluator.evaluate(new Template(Template.Type.HANDLEBARS, "{{incDate value}}"), mapper.createObjectNode().put("value", 0)).get());
    }


}
