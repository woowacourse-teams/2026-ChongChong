import z from 'zod';

const eventSchema = z.object({
  resourceId: z.number().int().positive(),
  title: z.string(),
  eventAt: z.iso.datetime({ local: true }).regex(/T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?$/),
});

const noticeEventSchema = eventSchema.extend({
  eventType: z.literal('NOTICE_CREATED'),
  resourceType: z.literal('NOTICE'),
  readStatus: z.enum(['NOT_ASSIGNED', 'UNREAD', 'READ']),
});

const assignmentEventSchema = eventSchema.extend({
  eventType: z.enum(['ASSIGNMENT_CREATED', 'ASSIGNMENT_DUE']),
  resourceType: z.literal('ASSIGNMENT'),
  submissionStatus: z.enum(['NOT_SUBMITTED', 'SUBMITTED', 'LATE_SUBMITTED', 'MISSING']),
});

const calendarEventSchema = z.discriminatedUnion('resourceType', [
  noticeEventSchema,
  assignmentEventSchema,
]);

const monthlyCalendarResponseSchema = z.object({
  year: z.number().int().min(1).max(9998),
  month: z.number().int().min(1).max(12),
  events: z.array(calendarEventSchema),
});

export type CalendarEvent = z.infer<typeof calendarEventSchema>;
export type MonthlyCalendarResponse = z.infer<typeof monthlyCalendarResponseSchema>;

export function isMonthlyCalendarResponse(data: unknown): data is MonthlyCalendarResponse {
  return monthlyCalendarResponseSchema.safeParse(data).success;
}
