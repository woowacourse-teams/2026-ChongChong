import { router } from 'expo-router';
import { Image, View } from 'react-native';
import { AppText, Button } from '../../ui/primitives';
import { ActivityIcon } from '../home/ActivityIcon';
import { LinkIcon } from './LinkIcon';
import type { Assignment, Submission } from './model';
import { assignmentStyles as s } from './styles';

export function SubmissionContent({
  submission,
}: {
  readonly submission: Submission;
}) {
  return (
    <>
      <AppText variant="caption" tone="tertiary">
        {submission.submittedAt} 제출
      </AppText>
      <View style={s.block}>
        <View style={s.row}>
          <ActivityIcon kind="assignment" />
          <AppText style={s.green}>내용</AppText>
        </View>
        <AppText tone="tertiary">{submission.body}</AppText>
      </View>
      {!!submission.link && (
        <View style={s.block}>
          <View style={s.row}>
            <LinkIcon green />
            <AppText style={s.green}>링크</AppText>
          </View>
          <AppText selectable tone="tertiary">
            {submission.link}
          </AppText>
        </View>
      )}
      {submission.files.map((file) => (
        <View key={file.id} style={s.file}>
          <AppText>{file.name}</AppText>
        </View>
      ))}
    </>
  );
}
export function AssignmentDetail({
  assignment,
}: {
  readonly assignment: Assignment;
}) {
  const self = assignment.members.find((person) => person.id === 'self');
  return (
    <View style={s.detail}>
      {[
        { title: '과제 내용', body: assignment.body },
        { title: '제출 방법', body: assignment.method },
      ].map((block) => (
        <View key={block.title} style={s.block}>
          <View style={s.row}>
            <ActivityIcon kind="assignment" />
            <AppText style={s.green}>{block.title}</AppText>
          </View>
          <AppText tone="tertiary">{block.body}</AppText>
        </View>
      ))}
      {assignment.images.map((image) => (
        <Image
          key={image.id}
          source={image.source}
          accessibilityLabel={image.name}
          resizeMode="contain"
          style={{ width: '100%', height: 250 }}
        />
      ))}
      {self && (
        <View style={s.selfSection}>
          <AppText variant="large">내 제출</AppText>
          {self.submission ? (
            <SubmissionContent submission={self.submission} />
          ) : (
            <View style={s.emptySubmission}>
              <AppText variant="caption" tone="tertiary">
                아직 과제를 제출하지 않았어요
              </AppText>
            </View>
          )}
          <Button
            label={self.submission ? '제출물 수정하기' : '과제 제출하기'}
            onPress={() =>
              router.push({
                pathname: '/assignments/submit',
                params: { id: assignment.id },
              })
            }
          />
        </View>
      )}
    </View>
  );
}
