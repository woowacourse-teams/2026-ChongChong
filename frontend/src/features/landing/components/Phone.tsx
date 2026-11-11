/** 얇은 베젤의 휴대폰 프레임. 화면 이미지는 장식이라 보조기기에는 label만 읽혀요. */
export default function Phone({ src, label }: { src: string; label: string }) {
  return (
    <div className="phone" role="img" aria-label={label}>
      <div className="screen">
        <img src={src} alt="" />
      </div>
    </div>
  );
}
