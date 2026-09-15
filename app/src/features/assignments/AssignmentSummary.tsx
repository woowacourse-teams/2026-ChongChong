import { router } from 'expo-router';
import { Pressable, View } from 'react-native';
import { AppText } from '../../ui/primitives';
import { NoticeIcon } from '../notices/NoticeIcon';
import { useAssignments } from './AssignmentProvider';
import type { Assignment, AssignmentMember } from './model';
import { assignmentStyles as s } from './styles';

export function AssignmentSummary({
  assignment,
  leader,
  onDetail,
}: {
  readonly assignment: Assignment;
  readonly leader: boolean;
  readonly onDetail: () => void;
}) {
  const { remind } = useAssignments();
  const complete = assignment.members.filter((person) => person.submission);
  const pending = assignment.members.filter((person) => !person.submission);
  const self = assignment.members.find((person) => person.id === 'self');
  const showAll = leader || assignment.visibility === 'public';
  const personCard = (person: AssignmentMember) => (
    <View key={person.id} style={s.person}>
      <NoticeIcon name="profile" size={28} />
      <View style={{ flex: 1 }}>
        <AppText>{person.name}</AppText>
        {(person.submission || (leader && person.remindedAt)) && (
          <AppText variant="compact" tone="tertiary">
            {person.submission
              ? `${person.submission.submittedAt} 제출`
              : `${person.remindedAt} 보냄`}
          </AppText>
        )}
      </View>
      {person.submission ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`${person.name} 제출 상세 보기`}
          onPress={() =>
            person.id === 'self'
              ? onDetail()
              : router.push({
                  pathname: '/assignments/submission',
                  params: { id: assignment.id, memberId: person.id },
                })
          }
        >
          <AppText variant="caption" tone="tertiary">
            상세 보기
          </AppText>
        </Pressable>
      ) : (
        leader &&
        person.remindedAt !== '방금' && (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${person.name}에게 알리기`}
            onPress={() => remind(assignment.id, person.id)}
            style={s.row}
          >
            <NoticeIcon name="send" size={16} />
            <AppText style={s.green}>알리기</AppText>
          </Pressable>
        )
      )}
    </View>
  );
  return (
    <View style={s.summary}>
      {showAll && (
        <View style={s.group}>
          <AppText variant="large">제출 현황</AppText>
          <View style={s.count}>
            <AppText strong style={s.number}>
              {complete.length}
            </AppText>
            <AppText tone="tertiary">/ {assignment.members.length}명</AppText>
          </View>
          <View style={s.track}>
            <View
              style={[
                s.progress,
                {
                  width: `${assignment.members.length ? (complete.length / assignment.members.length) * 100 : 0}%`,
                },
              ]}
            />
          </View>
        </View>
      )}
      <View style={s.section}>
        <AppText variant="large">내 제출</AppText>
        {self?.submission ? (
          personCard(self)
        ) : (
          <View style={s.emptySubmission}>
            <AppText variant="caption" tone="tertiary">
              {self ? '아직 과제를 제출하지 않았어요' : '제출 대상이 아니에요'}
            </AppText>
          </View>
        )}
      </View>
      {showAll && (
        <>
          <View style={s.section}>
            <AppText variant="large">제출 완료 {complete.length}명</AppText>
            {complete.map(personCard)}
          </View>
          <View style={s.section}>
            <View style={s.between}>
              <AppText variant="large">미제출 {pending.length}명</AppText>
              {leader && pending.length > 0 && (
                <Pressable
                  accessibilityRole="button"
                  onPress={() => remind(assignment.id)}
                >
                  <AppText style={s.green}>모두에게 알리기</AppText>
                </Pressable>
              )}
            </View>
            {pending.map(personCard)}
          </View>
        </>
      )}
    </View>
  );
}
