/* =========================================================================
 * College ERP System - single page frontend
 *
 * Iteration 5 (ERP integration): connects every module built in iterations
 * 1-4 behind one role-aware navigation shell.
 *
 * Vanilla JavaScript only - no framework, no build step and no CDN, so the
 * portal works offline exactly as served by Spring Boot from /static.
 *
 * Every backend response uses the envelope { success, data, error } and
 * authentication is a server session cookie, so every request is sent with
 * credentials: 'same-origin'.
 * ========================================================================= */
(function () {
  'use strict';

  /* ------------------------------------------------------------------ *
   * Application state
   * ------------------------------------------------------------------ */

  var state = {
    user: null,          // { username, fullName, role }
    courses: [],         // course catalogue currently loaded
    facultyCourses: [],  // courses assigned to the logged-in faculty member
    facultyList: [],     // faculty directory (admin screens)
    editingStudentId: null,
    editingFacultyId: null,
    editingCourseId: null
  };

  /* ------------------------------------------------------------------ *
   * Small DOM helpers
   * ------------------------------------------------------------------ */

  function $(id) {
    return document.getElementById(id);
  }

  function qsa(selector, root) {
    return Array.prototype.slice.call((root || document).querySelectorAll(selector));
  }

  /** Sets text content safely - never innerHTML with server data. */
  function text(value) {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    return String(value);
  }

  function cell(row, value) {
    var td = document.createElement('td');
    td.textContent = text(value);
    row.appendChild(td);
    return td;
  }

  function pill(row, value, okValue) {
    var td = document.createElement('td');
    var span = document.createElement('span');
    span.className = 'pill ' + (value === okValue ? 'pill-present' : 'pill-absent');
    span.textContent = text(value);
    td.appendChild(span);
    row.appendChild(td);
    return td;
  }

  function button(label, className, handler) {
    var btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn btn-sm ' + className;
    btn.textContent = label;
    btn.addEventListener('click', handler);
    return btn;
  }

  /** Empties a table body and, when there are no rows, shows a placeholder. */
  function tbodyOf(tableId) {
    var tbody = $(tableId).querySelector('tbody');
    tbody.textContent = '';
    return tbody;
  }

  function emptyRow(tbody, tableId, message) {
    var columns = $(tableId).querySelectorAll('thead th').length;
    var tr = document.createElement('tr');
    var td = document.createElement('td');
    td.className = 'empty-row';
    td.colSpan = columns;
    td.textContent = message || 'No records found.';
    tr.appendChild(td);
    tbody.appendChild(tr);
  }

  function formatDateTime(value) {
    if (!value) {
      return '—';
    }
    return String(value).replace('T', ' ').substring(0, 16);
  }

  /* ------------------------------------------------------------------ *
   * Alerts
   * ------------------------------------------------------------------ */

  var alertTimer = null;

  function showAlert(message, kind) {
    var box = state.user ? $('alert') : $('login-alert');
    box.textContent = message;
    box.className = 'alert ' + (kind === 'success' ? 'alert-success' : 'alert-error');
    box.hidden = false;

    if (alertTimer) {
      window.clearTimeout(alertTimer);
    }
    if (kind === 'success') {
      alertTimer = window.setTimeout(function () {
        box.hidden = true;
      }, 4000);
    }
  }

  function clearAlerts() {
    $('alert').hidden = true;
    $('login-alert').hidden = true;
  }

  /* ------------------------------------------------------------------ *
   * API layer
   *
   * One wrapper for every call. It attaches the session cookie, unwraps the
   * {success, data, error} envelope, throws when success is false, and sends
   * the user back to the login screen on HTTP 401.
   * ------------------------------------------------------------------ */

  function ApiError(message) {
    this.name = 'ApiError';
    this.message = message;
  }
  ApiError.prototype = Object.create(Error.prototype);

  function api(method, path, body) {
    var options = {
      method: method,
      credentials: 'same-origin',
      headers: { 'Accept': 'application/json' }
    };
    if (body !== undefined && body !== null) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }

    return fetch(path, options).then(function (response) {
      if (response.status === 401) {
        handleUnauthorized();
        throw new ApiError('Your session has expired. Please sign in again.');
      }
      if (response.status === 204) {
        return null;
      }

      return response.text().then(function (raw) {
        var payload = null;
        if (raw) {
          try {
            payload = JSON.parse(raw);
          } catch (e) {
            throw new ApiError('The server returned an unexpected response.');
          }
        }

        if (payload && typeof payload.success === 'boolean') {
          if (!payload.success) {
            throw new ApiError(payload.error || 'The request could not be completed.');
          }
          return payload.data;
        }

        if (!response.ok) {
          throw new ApiError('Request failed with status ' + response.status + '.');
        }
        return payload;
      });
    });
  }

  var get = function (path) { return api('GET', path); };
  var post = function (path, body) { return api('POST', path, body); };
  var put = function (path, body) { return api('PUT', path, body); };
  var del = function (path) { return api('DELETE', path); };

  /** Reports any rejected API promise into the alert area. */
  function fail(error) {
    if (error && error.name === 'ApiError') {
      showAlert(error.message, 'error');
    } else {
      showAlert('Unable to reach the server. Check that the application is running.', 'error');
    }
  }

  function query(params) {
    var parts = [];
    Object.keys(params).forEach(function (key) {
      var value = params[key];
      if (value !== undefined && value !== null && String(value).trim() !== '') {
        parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(String(value).trim()));
      }
    });
    return parts.length ? '?' + parts.join('&') : '';
  }

  function list(value) {
    return Array.isArray(value) ? value : [];
  }

  /* ------------------------------------------------------------------ *
   * Authentication and screen switching (FR-01, US-01)
   * ------------------------------------------------------------------ */

  function handleUnauthorized() {
    state.user = null;
    showLoginScreen();
  }

  function showLoginScreen() {
    $('app-screen').hidden = true;
    $('login-screen').hidden = false;
    $('login-password').value = '';
    $('sidebar').classList.remove('open');
  }

  function showAppScreen() {
    $('login-screen').hidden = true;
    $('app-screen').hidden = false;
    $('current-user').textContent = state.user.fullName || state.user.username;
    $('current-role').textContent = state.user.role;
    applyRoleNavigation();
  }

  /** Hides every nav item the current role may not use, then opens the first. */
  function applyRoleNavigation() {
    var role = state.user.role;
    var visible = [];

    qsa('.nav-item').forEach(function (item) {
      var allowed = item.getAttribute('data-roles').split(',');
      var permitted = allowed.indexOf(role) !== -1;
      item.hidden = !permitted;
      if (permitted) {
        visible.push(item);
      }
    });

    // Hide a group heading when none of the items under it are permitted.
    qsa('.nav-group-title').forEach(function (title) {
      var anyVisible = false;
      var node = title.nextElementSibling;
      while (node && node.classList.contains('nav-item')) {
        if (!node.hidden) {
          anyVisible = true;
        }
        node = node.nextElementSibling;
      }
      title.hidden = !anyVisible;
    });

    if (visible.length) {
      openSection(visible[0].getAttribute('data-section'));
    }
  }

  function login(event) {
    event.preventDefault();
    clearAlerts();

    var credentials = {
      username: $('login-username').value.trim(),
      password: $('login-password').value
    };
    if (!credentials.username || !credentials.password) {
      showAlert('Enter both a username and a password.', 'error');
      return;
    }

    post('/api/auth/login', credentials).then(function (user) {
      state.user = user;
      $('login-form').reset();
      clearAlerts();
      showAppScreen();
    }).catch(fail);
  }

  function logout() {
    post('/api/auth/logout').catch(function () {
      // The session is discarded locally regardless of the server's reply.
    }).then(function () {
      state.user = null;
      clearAlerts();
      showLoginScreen();
    });
  }

  /** Restores an existing session on page load so a refresh does not log out. */
  function restoreSession() {
    get('/api/auth/me').then(function (user) {
      if (user && user.username) {
        state.user = user;
        showAppScreen();
      } else {
        showLoginScreen();
      }
    }).catch(function () {
      showLoginScreen();
    });
  }

  /* ------------------------------------------------------------------ *
   * Navigation
   * ------------------------------------------------------------------ */

  var SECTION_LOADERS = {
    'profile': loadProfile,
    'register': loadRegistrationSection,
    'my-attendance': loadMyAttendance,
    'my-marks': loadMyMarks,
    'my-results': loadMyResults,
    'my-courses': loadFacultySection,
    'mark-attendance': loadAttendanceWorkspace,
    'enter-marks': loadMarksWorkspace,
    'publish-results': loadPublishWorkspace,
    'admin-students': loadAdminStudents,
    'admin-faculty': loadAdminFaculty,
    'admin-courses': loadAdminCourses
  };

  function openSection(name) {
    clearAlerts();

    qsa('.section').forEach(function (section) {
      section.hidden = section.id !== 'section-' + name;
    });
    qsa('.nav-item').forEach(function (item) {
      item.classList.toggle('active', item.getAttribute('data-section') === name);
    });
    $('sidebar').classList.remove('open');

    var loader = SECTION_LOADERS[name];
    if (loader) {
      loader();
    }
  }

  /* ------------------------------------------------------------------ *
   * Shared rendering helpers
   * ------------------------------------------------------------------ */

  function renderDetails(containerId, pairs) {
    var container = $(containerId);
    container.textContent = '';
    pairs.forEach(function (pair) {
      var wrapper = document.createElement('div');
      var label = document.createElement('div');
      label.className = 'label';
      label.textContent = pair[0];
      var value = document.createElement('div');
      value.className = 'value';
      value.textContent = text(pair[1]);
      wrapper.appendChild(label);
      wrapper.appendChild(value);
      container.appendChild(wrapper);
    });
  }

  function fillSelect(selectId, items, valueKey, labelFn, placeholder) {
    var select = $(selectId);
    if (!select) {
      return;
    }
    var previous = select.value;
    select.textContent = '';

    var blank = document.createElement('option');
    blank.value = '';
    blank.textContent = items.length ? (placeholder || 'Select…') : 'None available';
    select.appendChild(blank);

    items.forEach(function (item) {
      var option = document.createElement('option');
      option.value = item[valueKey];
      option.textContent = labelFn(item);
      select.appendChild(option);
    });

    if (previous) {
      select.value = previous;
    }
  }

  function courseLabel(course) {
    return course.code + ' — ' + course.title;
  }

  function studentLabel(student) {
    return student.rollNumber + ' — ' + student.name;
  }

  function renderCourseRows(tbody, tableId, courses, actionFn) {
    if (!courses.length) {
      emptyRow(tbody, tableId, 'No courses found.');
      return;
    }
    courses.forEach(function (course) {
      var tr = document.createElement('tr');
      cell(tr, course.code);
      cell(tr, course.title);
      cell(tr, course.credits);
      cell(tr, course.semester);
      cell(tr, course.department);
      cell(tr, course.facultyName);
      if (actionFn) {
        actionFn(tr, course);
      }
      tbody.appendChild(tr);
    });
  }

  /* ================================================================== *
   * STUDENT MODULES
   * ================================================================== */

  /** FR-02 - student profile. */
  function loadProfile() {
    get('/api/students/me').then(function (student) {
      renderDetails('profile-body', [
        ['Roll number', student.rollNumber],
        ['Name', student.name],
        ['Department', student.department],
        ['Semester', student.semester],
        ['Phone', student.phone],
        ['Username', student.username],
        ['Email', student.email]
      ]);
    }).catch(function (error) {
      $('profile-body').textContent = '';
      fail(error);
    });
  }

  /** FR-03 + FR-04 - browse the catalogue and register. */
  function loadRegistrationSection() {
    loadCatalogue();
    loadMyEnrollments();
  }

  function loadCatalogue() {
    var params = query({
      department: $('filter-department').value,
      semester: $('filter-semester').value
    });

    get('/api/courses' + params).then(function (courses) {
      state.courses = list(courses);
      var tbody = tbodyOf('catalog-table');
      renderCourseRows(tbody, 'catalog-table', state.courses, function (tr, course) {
        var td = document.createElement('td');
        td.className = 'actions';
        td.appendChild(button('Register', 'btn-primary', function () {
          registerForCourse(course.id);
        }));
        tr.appendChild(td);
      });
    }).catch(fail);
  }

  function registerForCourse(courseId) {
    post('/api/enrollments/register', { courseId: courseId }).then(function (enrollment) {
      showAlert('Registered for ' + enrollment.course.code + ' — ' + enrollment.course.title + '.', 'success');
      loadMyEnrollments();
    }).catch(fail);
  }

  function loadMyEnrollments() {
    get('/api/enrollments/me').then(function (enrollments) {
      var rows = list(enrollments);
      var tbody = tbodyOf('my-enrollments-table');
      if (!rows.length) {
        emptyRow(tbody, 'my-enrollments-table', 'You have not registered for any course yet.');
        return;
      }
      rows.forEach(function (enrollment) {
        var course = enrollment.course || {};
        var tr = document.createElement('tr');
        cell(tr, course.code);
        cell(tr, course.title);
        cell(tr, course.credits);
        cell(tr, course.semester);
        cell(tr, enrollment.registeredOn);
        var td = document.createElement('td');
        td.className = 'actions';
        td.appendChild(button('Drop', 'btn-danger', function () {
          dropEnrollment(enrollment.id, course.code);
        }));
        tr.appendChild(td);
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  function dropEnrollment(id, code) {
    if (!window.confirm('Drop the registration for ' + code + '?')) {
      return;
    }
    del('/api/enrollments/' + id).then(function () {
      showAlert('Registration for ' + code + ' was removed.', 'success');
      loadMyEnrollments();
    }).catch(fail);
  }

  /** FR-05 / US-03 - the student's own attendance. */
  function loadMyAttendance() {
    get('/api/attendance/me/summary').then(function (summaries) {
      var rows = list(summaries);
      var tbody = tbodyOf('attendance-summary-table');
      if (!rows.length) {
        emptyRow(tbody, 'attendance-summary-table', 'No attendance has been recorded yet.');
        return;
      }
      rows.forEach(function (summary) {
        var tr = document.createElement('tr');
        cell(tr, summary.courseCode);
        cell(tr, summary.courseTitle);
        cell(tr, summary.totalClasses);
        cell(tr, summary.presentCount);
        cell(tr, summary.percentage + '%');
        tbody.appendChild(tr);
      });
    }).catch(fail);

    get('/api/attendance/me').then(function (records) {
      var rows = list(records);
      var tbody = tbodyOf('my-attendance-table');
      if (!rows.length) {
        emptyRow(tbody, 'my-attendance-table', 'No attendance records yet.');
        return;
      }
      rows.forEach(function (record) {
        var tr = document.createElement('tr');
        cell(tr, record.classDate);
        cell(tr, record.courseCode);
        cell(tr, record.courseTitle);
        pill(tr, record.status, 'PRESENT');
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  /** FR-06 - the student's own marks. */
  function loadMyMarks() {
    get('/api/marks/me').then(function (marks) {
      var rows = list(marks);
      var tbody = tbodyOf('my-marks-table');
      if (!rows.length) {
        emptyRow(tbody, 'my-marks-table', 'No marks have been entered yet.');
        return;
      }
      rows.forEach(function (mark) {
        var tr = document.createElement('tr');
        cell(tr, mark.courseCode);
        cell(tr, mark.courseTitle);
        cell(tr, mark.internalMarks);
        cell(tr, mark.externalMarks);
        cell(tr, mark.total);
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  /** Fills the aggregate card above the results table. */
  function renderResultSummary(summary) {
    var data = summary || {};
    $('summary-total-courses').textContent = text(data.totalCourses);
    $('summary-courses-passed').textContent = text(data.coursesPassed);
    $('summary-total-marks').textContent = text(data.totalMarks);
    $('summary-percentage').textContent =
      (data.percentage === null || data.percentage === undefined)
        ? text(null)
        : data.percentage + '%';
  }

  /**
   * FR-07 / US-05 - the student's published results.
   *
   * The endpoint returns {results, summary} rather than a bare array, so the
   * rows are read off payload.results and the aggregate is rendered alongside.
   */
  function loadMyResults() {
    get('/api/results/me').then(function (payload) {
      var data = payload || {};
      var rows = list(data.results);
      renderResultSummary(data.summary);
      var tbody = tbodyOf('my-results-table');
      if (!rows.length) {
        emptyRow(tbody, 'my-results-table', 'No results have been published yet.');
        return;
      }
      rows.forEach(function (result) {
        var tr = document.createElement('tr');
        cell(tr, result.courseCode);
        cell(tr, result.courseTitle);
        cell(tr, result.totalMarks);
        cell(tr, result.grade);
        pill(tr, result.passed ? 'PASS' : 'FAIL', 'PASS');
        cell(tr, formatDateTime(result.publishedOn));
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  /* ================================================================== *
   * FACULTY MODULES
   * ================================================================== */

  /**
   * Loads the courses assigned to the signed-in faculty member and mirrors
   * them into every course picker on the faculty screens.
   */
  function loadFacultyCourses() {
    return get('/api/faculty/me').then(function (faculty) {
      return get('/api/courses').then(function (courses) {
        state.facultyCourses = list(courses).filter(function (course) {
          return course.facultyId === faculty.id;
        });
        return faculty;
      });
    }).then(function (faculty) {
      ['roster-course-id', 'att-course-id', 'bulk-course-id', 'reg-course-id',
        'marks-course-id', 'marks-view-course-id', 'publish-course-id'
      ].forEach(function (id) {
        fillSelect(id, state.facultyCourses, 'id', courseLabel, 'Select a course…');
      });
      return faculty;
    });
  }

  /** FR-08 - faculty profile and the courses they are responsible for. */
  function loadFacultySection() {
    loadFacultyCourses().then(function (faculty) {
      renderDetails('faculty-profile-body', [
        ['Employee code', faculty.employeeCode],
        ['Name', faculty.name],
        ['Department', faculty.department],
        ['Designation', faculty.designation],
        ['Username', faculty.username],
        ['Email', faculty.email]
      ]);

      var tbody = tbodyOf('faculty-courses-table');
      renderCourseRows(tbody, 'faculty-courses-table', state.facultyCourses, function (tr, course) {
        var td = document.createElement('td');
        td.className = 'actions';
        td.appendChild(button('View roster', 'btn-ghost', function () {
          $('roster-course-id').value = course.id;
          loadRoster(course.id);
        }));
        tr.appendChild(td);
      });

      tbodyOf('roster-table');
      emptyRow($('roster-table').querySelector('tbody'), 'roster-table', 'Select a course to view its roster.');
    }).catch(fail);
  }

  function loadRoster(courseId) {
    if (!courseId) {
      showAlert('Select a course first.', 'error');
      return Promise.resolve([]);
    }
    return get('/api/enrollments/course/' + courseId).then(function (enrollments) {
      var rows = list(enrollments);
      var tbody = tbodyOf('roster-table');
      if (!rows.length) {
        emptyRow(tbody, 'roster-table', 'No students are registered for this course.');
      } else {
        rows.forEach(function (enrollment) {
          var tr = document.createElement('tr');
          cell(tr, enrollment.rollNumber);
          cell(tr, enrollment.studentName);
          cell(tr, enrollment.registeredOn);
          tbody.appendChild(tr);
        });
      }
      return rows;
    }).catch(function (error) {
      fail(error);
      return [];
    });
  }

  /** Populates a student picker with the students registered for a course. */
  function loadEnrolledStudents(courseId, selectId) {
    if (!courseId) {
      fillSelect(selectId, [], 'studentId', studentLabel);
      return Promise.resolve([]);
    }
    return get('/api/enrollments/course/' + courseId).then(function (enrollments) {
      var students = list(enrollments).map(function (enrollment) {
        return {
          studentId: enrollment.studentId,
          rollNumber: enrollment.rollNumber,
          name: enrollment.studentName
        };
      });
      fillSelect(selectId, students, 'studentId', studentLabel, 'Select a student…');
      return students;
    }).catch(function (error) {
      fail(error);
      return [];
    });
  }

  /** FR-05 - attendance entry workspace. */
  function loadAttendanceWorkspace() {
    loadFacultyCourses().then(function () {
      var today = new Date().toISOString().substring(0, 10);
      if (!$('att-date').value) {
        $('att-date').value = today;
      }
      if (!$('bulk-date').value) {
        $('bulk-date').value = today;
      }
    }).catch(fail);
  }

  function submitAttendance(event) {
    event.preventDefault();
    var payload = {
      studentId: Number($('att-student-id').value),
      courseId: Number($('att-course-id').value),
      classDate: $('att-date').value,
      status: $('att-status').value
    };
    if (!payload.studentId || !payload.courseId || !payload.classDate) {
      showAlert('Select a course, a student and a class date.', 'error');
      return;
    }

    post('/api/attendance', payload).then(function (record) {
      showAlert('Attendance saved for ' + record.studentName + ' on ' + record.classDate + '.', 'success');
    }).catch(fail);
  }

  function loadBulkClassList() {
    var courseId = $('bulk-course-id').value;
    if (!courseId) {
      showAlert('Select a course first.', 'error');
      return;
    }

    get('/api/enrollments/course/' + courseId).then(function (enrollments) {
      var rows = list(enrollments);
      var tbody = tbodyOf('bulk-attendance-table');
      if (!rows.length) {
        emptyRow(tbody, 'bulk-attendance-table', 'No students are registered for this course.');
        return;
      }
      rows.forEach(function (enrollment) {
        var tr = document.createElement('tr');
        tr.setAttribute('data-student-id', enrollment.studentId);
        cell(tr, enrollment.rollNumber);
        cell(tr, enrollment.studentName);

        var td = document.createElement('td');
        var select = document.createElement('select');
        select.className = 'bulk-status';
        ['PRESENT', 'ABSENT'].forEach(function (status) {
          var option = document.createElement('option');
          option.value = status;
          option.textContent = status;
          select.appendChild(option);
        });
        td.appendChild(select);
        tr.appendChild(td);
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  function submitBulkAttendance(event) {
    event.preventDefault();
    var courseId = Number($('bulk-course-id').value);
    var classDate = $('bulk-date').value;
    if (!courseId || !classDate) {
      showAlert('Select a course and a class date.', 'error');
      return;
    }

    var entries = qsa('#bulk-attendance-table tbody tr[data-student-id]').map(function (tr) {
      return {
        studentId: Number(tr.getAttribute('data-student-id')),
        status: tr.querySelector('.bulk-status').value
      };
    });
    if (!entries.length) {
      showAlert('Load the class list before submitting attendance.', 'error');
      return;
    }

    post('/api/attendance/bulk', {
      courseId: courseId,
      classDate: classDate,
      entries: entries
    }).then(function (records) {
      showAlert('Attendance recorded for ' + list(records).length + ' student(s).', 'success');
    }).catch(fail);
  }

  function loadCourseAttendance(event) {
    event.preventDefault();
    var courseId = $('reg-course-id').value;
    if (!courseId) {
      showAlert('Select a course first.', 'error');
      return;
    }

    get('/api/attendance/course/' + courseId + query({ date: $('reg-date').value }))
      .then(function (records) {
        var rows = list(records);
        var tbody = tbodyOf('course-attendance-table');
        if (!rows.length) {
          emptyRow(tbody, 'course-attendance-table', 'No attendance recorded for this selection.');
          return;
        }
        rows.forEach(function (record) {
          var tr = document.createElement('tr');
          cell(tr, record.classDate);
          cell(tr, record.rollNumber);
          cell(tr, record.studentName);
          pill(tr, record.status, 'PRESENT');

          var td = document.createElement('td');
          td.className = 'actions';
          var flipTo = record.status === 'PRESENT' ? 'ABSENT' : 'PRESENT';
          td.appendChild(button('Mark ' + flipTo, 'btn-ghost', function () {
            correctAttendance(record, flipTo);
          }));
          tr.appendChild(td);
          tbody.appendChild(tr);
        });
      }).catch(fail);
  }

  function correctAttendance(record, status) {
    put('/api/attendance/' + record.id, {
      studentId: record.studentId,
      courseId: record.courseId,
      classDate: record.classDate,
      status: status
    }).then(function () {
      showAlert('Attendance updated for ' + record.studentName + '.', 'success');
      $('course-attendance-form').dispatchEvent(new Event('submit', { cancelable: true }));
    }).catch(fail);
  }

  /** FR-06 / US-04 - marks entry workspace. */
  function loadMarksWorkspace() {
    loadFacultyCourses().catch(fail);
  }

  function submitMarks(event) {
    event.preventDefault();
    var payload = {
      studentId: Number($('marks-student-id').value),
      courseId: Number($('marks-course-id').value),
      internalMarks: Number($('marks-internal').value),
      externalMarks: Number($('marks-external').value)
    };
    if (!payload.studentId || !payload.courseId) {
      showAlert('Select a course and a student.', 'error');
      return;
    }

    post('/api/marks', payload).then(function (mark) {
      showAlert('Marks saved: total ' + mark.total + ' out of 100.', 'success');
      $('marks-internal').value = '';
      $('marks-external').value = '';
      if ($('marks-view-course-id').value === String(payload.courseId)) {
        loadCourseMarks(payload.courseId);
      }
    }).catch(fail);
  }

  function loadCourseMarks(courseId) {
    return get('/api/marks/course/' + courseId).then(function (marks) {
      var rows = list(marks);
      var tbody = tbodyOf('course-marks-table');
      if (!rows.length) {
        emptyRow(tbody, 'course-marks-table', 'No marks recorded for this course yet.');
        return;
      }
      rows.forEach(function (mark) {
        var tr = document.createElement('tr');
        cell(tr, mark.rollNumber);
        cell(tr, mark.internalMarks);
        cell(tr, mark.externalMarks);
        cell(tr, mark.total);

        var td = document.createElement('td');
        td.className = 'actions';
        td.appendChild(button('Edit', 'btn-ghost', function () {
          $('marks-course-id').value = mark.courseId;
          loadEnrolledStudents(mark.courseId, 'marks-student-id').then(function () {
            $('marks-student-id').value = mark.studentId;
          });
          $('marks-internal').value = mark.internalMarks;
          $('marks-external').value = mark.externalMarks;
          showAlert('Editing marks for ' + mark.rollNumber + '. Saving overwrites the existing entry.', 'success');
        }));
        tr.appendChild(td);
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  /** FR-07 - publishing results for a whole course. */
  function loadPublishWorkspace() {
    loadFacultyCourses().catch(fail);
  }

  function publishResults(event) {
    event.preventDefault();
    var courseId = $('publish-course-id').value;
    if (!courseId) {
      showAlert('Select a course first.', 'error');
      return;
    }

    // Publishing returns a confirmation message, so the published rows are read
    // back from the course results endpoint afterwards.
    post('/api/results/publish/' + courseId).then(function (message) {
      showAlert(text(message), 'success');
      return get('/api/results/course/' + courseId);
    }).then(function (results) {
      renderPublishedResults(list(results));
    }).catch(fail);
  }

  function renderPublishedResults(rows) {
    var tbody = tbodyOf('published-results-table');
    if (!rows.length) {
      emptyRow(tbody, 'published-results-table', 'No results were published. Enter marks first.');
      return;
    }
    rows.forEach(function (result) {
      var tr = document.createElement('tr');
      cell(tr, result.rollNumber);
      cell(tr, result.totalMarks);
      cell(tr, result.grade);
      pill(tr, result.passed ? 'PASS' : 'FAIL', 'PASS');
      cell(tr, formatDateTime(result.publishedOn));
      tbody.appendChild(tr);
    });
  }

  /* ================================================================== *
   * ADMINISTRATOR MODULES
   * ================================================================== */

  /** FR-02 - student administration. */
  function loadAdminStudents() {
    loadStudentDirectory();
  }

  function loadStudentDirectory() {
    var params = query({ department: $('student-filter-department').value });

    get('/api/students' + params).then(function (students) {
      var rows = list(students);
      var tbody = tbodyOf('students-table');
      if (!rows.length) {
        emptyRow(tbody, 'students-table', 'No students found.');
        return;
      }
      rows.forEach(function (student) {
        var tr = document.createElement('tr');
        cell(tr, student.rollNumber);
        cell(tr, student.name);
        cell(tr, student.department);
        cell(tr, student.semester);
        cell(tr, student.phone);
        cell(tr, student.username);
        cell(tr, student.email);

        var td = document.createElement('td');
        td.className = 'actions';
        td.appendChild(button('Edit', 'btn-ghost', function () {
          startStudentEdit(student);
        }));
        td.appendChild(button('Delete', 'btn-danger', function () {
          deleteStudent(student);
        }));
        tr.appendChild(td);
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  function startStudentEdit(student) {
    state.editingStudentId = student.id;
    $('student-form-title').textContent = 'Edit student ' + student.rollNumber;
    $('student-id').value = student.id;
    $('student-roll').value = student.rollNumber;
    $('student-name').value = student.name;
    $('student-department').value = student.department;
    $('student-semester').value = student.semester;
    $('student-phone').value = student.phone || '';
    $('student-email').value = student.email;
    $('student-username').value = student.username;
    $('student-password').value = '';
    $('student-cancel').hidden = false;
    window.scrollTo(0, 0);
  }

  function resetStudentForm() {
    state.editingStudentId = null;
    $('student-form').reset();
    $('student-id').value = '';
    $('student-form-title').textContent = 'Add a student';
    $('student-cancel').hidden = true;
  }

  function submitStudent(event) {
    event.preventDefault();
    var payload = {
      rollNumber: $('student-roll').value.trim(),
      name: $('student-name').value.trim(),
      department: $('student-department').value.trim(),
      semester: Number($('student-semester').value),
      phone: $('student-phone').value.trim(),
      username: $('student-username').value.trim(),
      password: $('student-password').value,
      email: $('student-email').value.trim()
    };

    var request = state.editingStudentId
      ? put('/api/students/' + state.editingStudentId, payload)
      : post('/api/students', payload);

    request.then(function (student) {
      showAlert('Student ' + student.rollNumber + ' saved.', 'success');
      resetStudentForm();
      loadStudentDirectory();
    }).catch(fail);
  }

  function deleteStudent(student) {
    if (!window.confirm('Delete student ' + student.rollNumber + ' (' + student.name + ')?')) {
      return;
    }
    del('/api/students/' + student.id).then(function () {
      showAlert('Student ' + student.rollNumber + ' was deleted.', 'success');
      if (state.editingStudentId === student.id) {
        resetStudentForm();
      }
      loadStudentDirectory();
    }).catch(fail);
  }

  /** FR-08 - faculty administration. */
  function loadAdminFaculty() {
    loadFacultyDirectory();
  }

  function loadFacultyDirectory() {
    var params = query({ department: $('faculty-filter-department').value });

    return get('/api/faculty' + params).then(function (faculty) {
      state.facultyList = list(faculty);
      var tbody = tbodyOf('faculty-table');
      if (!state.facultyList.length) {
        emptyRow(tbody, 'faculty-table', 'No faculty found.');
        return;
      }
      state.facultyList.forEach(function (member) {
        var tr = document.createElement('tr');
        cell(tr, member.employeeCode);
        cell(tr, member.name);
        cell(tr, member.department);
        cell(tr, member.designation);
        cell(tr, member.username);
        cell(tr, member.email);

        var td = document.createElement('td');
        td.className = 'actions';
        td.appendChild(button('Edit', 'btn-ghost', function () {
          startFacultyEdit(member);
        }));
        td.appendChild(button('Delete', 'btn-danger', function () {
          deleteFaculty(member);
        }));
        tr.appendChild(td);
        tbody.appendChild(tr);
      });
    }).catch(fail);
  }

  function startFacultyEdit(member) {
    state.editingFacultyId = member.id;
    $('faculty-form-title').textContent = 'Edit faculty ' + member.employeeCode;
    $('faculty-id').value = member.id;
    $('faculty-code').value = member.employeeCode;
    $('faculty-name').value = member.name;
    $('faculty-department').value = member.department;
    $('faculty-designation').value = member.designation || '';
    $('faculty-email').value = member.email;
    $('faculty-username').value = member.username;
    $('faculty-password').value = '';
    $('faculty-cancel').hidden = false;
    window.scrollTo(0, 0);
  }

  function resetFacultyForm() {
    state.editingFacultyId = null;
    $('faculty-form').reset();
    $('faculty-id').value = '';
    $('faculty-form-title').textContent = 'Add a faculty member';
    $('faculty-cancel').hidden = true;
  }

  function submitFaculty(event) {
    event.preventDefault();
    var payload = {
      employeeCode: $('faculty-code').value.trim(),
      name: $('faculty-name').value.trim(),
      department: $('faculty-department').value.trim(),
      designation: $('faculty-designation').value.trim(),
      username: $('faculty-username').value.trim(),
      password: $('faculty-password').value,
      email: $('faculty-email').value.trim()
    };

    var request = state.editingFacultyId
      ? put('/api/faculty/' + state.editingFacultyId, payload)
      : post('/api/faculty', payload);

    request.then(function (member) {
      showAlert('Faculty ' + member.employeeCode + ' saved.', 'success');
      resetFacultyForm();
      loadFacultyDirectory();
    }).catch(fail);
  }

  function deleteFaculty(member) {
    if (!window.confirm('Delete faculty ' + member.employeeCode + ' (' + member.name + ')?')) {
      return;
    }
    del('/api/faculty/' + member.id).then(function () {
      showAlert('Faculty ' + member.employeeCode + ' was deleted.', 'success');
      if (state.editingFacultyId === member.id) {
        resetFacultyForm();
      }
      loadFacultyDirectory();
    }).catch(fail);
  }

  /** FR-03 - course administration. */
  function loadAdminCourses() {
    loadAdminCourseTable();
    loadFacultyDirectory().then(function () {
      fillSelect('assign-faculty-id', state.facultyList, 'id', function (member) {
        return member.employeeCode + ' — ' + member.name;
      }, 'Select a faculty member…');
    });
  }

  function loadAdminCourseTable() {
    var params = query({
      department: $('admin-filter-department').value,
      semester: $('admin-filter-semester').value
    });

    return get('/api/courses' + params).then(function (courses) {
      state.courses = list(courses);
      fillSelect('assign-course-id', state.courses, 'id', courseLabel, 'Select a course…');

      var tbody = tbodyOf('admin-courses-table');
      renderCourseRows(tbody, 'admin-courses-table', state.courses, function (tr, course) {
        var td = document.createElement('td');
        td.className = 'actions';
        td.appendChild(button('Edit', 'btn-ghost', function () {
          startCourseEdit(course);
        }));
        td.appendChild(button('Delete', 'btn-danger', function () {
          deleteCourse(course);
        }));
        tr.appendChild(td);
      });
    }).catch(fail);
  }

  function startCourseEdit(course) {
    state.editingCourseId = course.id;
    $('admin-course-form-title').textContent = 'Edit course ' + course.code;
    $('admin-course-id').value = course.id;
    $('admin-course-code').value = course.code;
    $('admin-course-title').value = course.title;
    $('admin-course-credits').value = course.credits;
    $('admin-course-semester').value = course.semester;
    $('admin-course-department').value = course.department;
    $('admin-course-cancel').hidden = false;
    window.scrollTo(0, 0);
  }

  function resetCourseForm() {
    state.editingCourseId = null;
    $('admin-course-form').reset();
    $('admin-course-id').value = '';
    $('admin-course-form-title').textContent = 'Add a course';
    $('admin-course-cancel').hidden = true;
  }

  function submitCourse(event) {
    event.preventDefault();
    var payload = {
      code: $('admin-course-code').value.trim(),
      title: $('admin-course-title').value.trim(),
      credits: Number($('admin-course-credits').value),
      semester: Number($('admin-course-semester').value),
      department: $('admin-course-department').value.trim()
    };

    var request = state.editingCourseId
      ? put('/api/courses/' + state.editingCourseId, payload)
      : post('/api/courses', payload);

    request.then(function (course) {
      showAlert('Course ' + course.code + ' saved.', 'success');
      resetCourseForm();
      loadAdminCourseTable();
    }).catch(fail);
  }

  function deleteCourse(course) {
    if (!window.confirm('Delete course ' + course.code + ' (' + course.title + ')?')) {
      return;
    }
    del('/api/courses/' + course.id).then(function () {
      showAlert('Course ' + course.code + ' was deleted.', 'success');
      if (state.editingCourseId === course.id) {
        resetCourseForm();
      }
      loadAdminCourseTable();
    }).catch(fail);
  }

  function assignFaculty(event) {
    event.preventDefault();
    var courseId = $('assign-course-id').value;
    var facultyId = $('assign-faculty-id').value;
    if (!courseId || !facultyId) {
      showAlert('Select both a course and a faculty member.', 'error');
      return;
    }

    put('/api/courses/' + courseId + '/faculty/' + facultyId).then(function (course) {
      showAlert(course.facultyName + ' was assigned to ' + course.code + '.', 'success');
      loadAdminCourseTable();
    }).catch(fail);
  }

  /* ------------------------------------------------------------------ *
   * Event wiring
   * ------------------------------------------------------------------ */

  function on(id, event, handler) {
    var element = $(id);
    if (element) {
      element.addEventListener(event, handler);
    }
  }

  function wire() {
    // Authentication
    on('login-form', 'submit', login);
    on('logout-btn', 'click', logout);

    // Navigation
    qsa('.nav-item').forEach(function (item) {
      item.addEventListener('click', function () {
        openSection(item.getAttribute('data-section'));
      });
    });
    on('menu-toggle', 'click', function () {
      $('sidebar').classList.toggle('open');
    });

    // Student - courses and registration
    on('course-filter-form', 'submit', function (event) {
      event.preventDefault();
      loadCatalogue();
    });
    on('filter-reset', 'click', function () {
      $('course-filter-form').reset();
      loadCatalogue();
    });

    // Faculty - roster
    on('roster-form', 'submit', function (event) {
      event.preventDefault();
      loadRoster($('roster-course-id').value);
    });

    // Faculty - attendance
    on('att-course-id', 'change', function () {
      loadEnrolledStudents($('att-course-id').value, 'att-student-id');
    });
    on('attendance-form', 'submit', submitAttendance);
    on('bulk-load', 'click', loadBulkClassList);
    on('bulk-attendance-form', 'submit', submitBulkAttendance);
    on('course-attendance-form', 'submit', loadCourseAttendance);

    // Faculty - marks
    on('marks-course-id', 'change', function () {
      loadEnrolledStudents($('marks-course-id').value, 'marks-student-id');
    });
    on('marks-form', 'submit', submitMarks);
    on('marks-view-form', 'submit', function (event) {
      event.preventDefault();
      var courseId = $('marks-view-course-id').value;
      if (!courseId) {
        showAlert('Select a course first.', 'error');
        return;
      }
      loadCourseMarks(courseId);
    });

    // Faculty - results
    on('publish-form', 'submit', publishResults);

    // Admin - students
    on('student-form', 'submit', submitStudent);
    on('student-cancel', 'click', resetStudentForm);
    on('student-filter-form', 'submit', function (event) {
      event.preventDefault();
      loadStudentDirectory();
    });
    on('student-filter-reset', 'click', function () {
      $('student-filter-form').reset();
      loadStudentDirectory();
    });

    // Admin - faculty
    on('faculty-form', 'submit', submitFaculty);
    on('faculty-cancel', 'click', resetFacultyForm);
    on('faculty-filter-form', 'submit', function (event) {
      event.preventDefault();
      loadFacultyDirectory();
    });
    on('faculty-filter-reset', 'click', function () {
      $('faculty-filter-form').reset();
      loadFacultyDirectory();
    });

    // Admin - courses
    on('admin-course-form', 'submit', submitCourse);
    on('admin-course-cancel', 'click', resetCourseForm);
    on('assign-faculty-form', 'submit', assignFaculty);
    on('admin-course-filter-form', 'submit', function (event) {
      event.preventDefault();
      loadAdminCourseTable();
    });
    on('admin-course-filter-reset', 'click', function () {
      $('admin-course-filter-form').reset();
      loadAdminCourseTable();
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    wire();
    restoreSession();
  });
})();
