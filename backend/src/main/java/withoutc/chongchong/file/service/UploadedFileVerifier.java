package withoutc.chongchong.file.service;

import withoutc.chongchong.file.entity.FileMetadata;

public interface UploadedFileVerifier {

    /**
     * 실제 저장소의 파일 존재 여부와 크기·형식 등 업로드 결과를 검증한다.
     * 검증에 실패하거나 결과를 확인할 수 없으면 예외를 발생시킨다.
     */
    void verify(FileMetadata file);
}
