
package co.udea.crowdfunding.service;

public record MilestoneEmailEvent(
        String email,
        String campaignName,
        int percentage) {
}
