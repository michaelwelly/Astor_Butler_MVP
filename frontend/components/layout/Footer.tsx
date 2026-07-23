export function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer>
      <a className="brand" href="#top">
        C3<span>FLEX</span><sup>.com</sup>
      </a>
      <p>Независимая продакшн-студия / {year}</p>
      <div>
        <a href="#catalog">Работы</a>
        <a href="#contact">Контакт</a>
        <a href="/studio">Приёмка</a>
      </div>
    </footer>
  );
}
