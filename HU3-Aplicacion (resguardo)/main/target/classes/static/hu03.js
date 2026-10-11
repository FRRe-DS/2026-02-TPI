document.querySelectorAll('input[type="file"]').forEach((input) => {
  input.addEventListener('change', () => {
    const label = input.closest('label');
    const fileName = label.querySelector('[data-file-name]');
    fileName.textContent = input.files.length ? input.files[0].name : 'ningún archivo';
  });
});
