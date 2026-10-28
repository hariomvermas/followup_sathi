package com.followupsaathi.config;

import com.followupsaathi.entity.MessageTemplate;
import java.util.List;

/**
 * Message templates copied into every new business during onboarding, so the
 * Templates page is never empty on first use. Owners edit or delete them
 * freely; seeding happens exactly once.
 */
public final class TemplateDefaults {

    private TemplateDefaults() {
    }

    public static List<MessageTemplate> forBusiness(
            com.followupsaathi.entity.Business business) {
        MessageTemplate welcome = new MessageTemplate();
        welcome.setBusiness(business);
        welcome.setTemplateName("Welcome / first contact");
        welcome.setMessageBody("""
                Hi {{customerName}}, thank you for visiting {{businessName}}! \
                You asked about {{serviceName}}. We will call you again \
                tomorrow to answer any questions. Reply here anytime.""");
        welcome.setDefault(true);

        MessageTemplate reminder = new MessageTemplate();
        reminder.setBusiness(business);
        reminder.setTemplateName("Follow-up reminder");
        reminder.setMessageBody("""
                Hello {{customerName}}, this is {{businessName}} following up \
                about {{serviceName}}. Are you available for a quick call \
                today?""");
        reminder.setDefault(true);

        MessageTemplate offer = new MessageTemplate();
        offer.setBusiness(business);
        offer.setTemplateName("Offer / discount");
        offer.setMessageBody("""
                Hi {{customerName}}, {{businessName}} is offering a special \
                price on {{serviceName}} this week. Would you like the \
                details?""");
        offer.setDefault(true);

        MessageTemplate appointment = new MessageTemplate();
        appointment.setBusiness(business);
        appointment.setTemplateName("Appointment reminder");
        appointment.setMessageBody("""
                Hello {{customerName}}, a gentle reminder from {{businessName}} \
                about our appointment on {{appointmentDate}} regarding \
                {{serviceName}}. See you then!""");
        appointment.setDefault(true);

        return List.of(welcome, reminder, offer, appointment);
    }
}
