// const STUDY_NAME_LIMIT_LENGTH = 15;
// const STUDY_DESCRIPTION_LIMIT_LENGTH = 30;

// interface StudyInput {
//   name: string;
//   description: string;
// }

// type StudyErrors = {
//   [K in keyof StudyInput]: FieldError;
// };

// type FieldError = { state: boolean; message: string };

// type FieldValidator = (value: string) => FieldError | null;

// const studyValidator = {
//   name: (value) => {
//     if (value.trim().length === 0) {
//       return { state: true, message: '스터디 이름은 필수입니다.' };
//     }

//     if (value.length > STUDY_NAME_LIMIT_LENGTH) {
//       return {
//         state: true,
//         message: `스터디 이름은 ${STUDY_NAME_LIMIT_LENGTH}자 이하만 가능해요.`,
//       };
//     }
//     return { state: false, message: '' };
//   },

//   description: (value) => {
//     if (value.length > STUDY_DESCRIPTION_LIMIT_LENGTH) {
//       return {
//         state: true,
//         message: `설명은 ${STUDY_DESCRIPTION_LIMIT_LENGTH}자 이하만 가능해요.`,
//       };
//     }
//     return { state: false, message: '' };
//   },
// } satisfies Record<keyof StudyInput, FieldValidator>;

// const studyFields = Object.keys(studyValidator) as (keyof StudyInput)[];

// export function validateStudy(input: Partial<StudyInput>): StudyErrors {
//   return studyFields.reduce((errors, field) => {
//     errors[field] = studyValidator[field](input[field] ?? '');
//     return errors;
//   }, {} as StudyErrors);
// }
