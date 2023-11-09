CREATE OR REPLACE FUNCTION archive_authority_record()
  RETURNS TRIGGER
  AS
'
BEGIN
  IF OLD.deleted = false AND NEW.deleted = true THEN
    INSERT INTO authority_archive(id, natural_id, source_file_id, source, heading, heading_type, _version,
      subject_heading_code, sft_headings, saft_headings, identifiers, notes, deleted, created_date, updated_date,
      created_by_user_id, updated_by_user_id)
    VALUES(NEW.id, NEW.natural_id, NEW.source_file_id, NEW.source, NEW.heading, NEW.heading_type, NEW._version,
                NEW.subject_heading_code, NEW.sft_headings, NEW.saft_headings, NEW.identifiers, NEW.notes, NEW.deleted,
                NEW.created_date, NEW.updated_date, NEW.created_by_user_id, NEW.updated_by_user_id);
  END IF;

  RETURN NEW;
END;
'
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS archive_authority_on_soft_delete ON authority CASCADE;
CREATE TRIGGER archive_authority_on_soft_delete
  AFTER UPDATE OF deleted
  ON authority
  FOR EACH ROW
  EXECUTE FUNCTION archive_authority_record();
