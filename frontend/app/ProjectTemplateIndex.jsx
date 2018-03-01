import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';
import ProjectTypeView from './EntryViews/ProjectTypeView.jsx';
import FileEntryView from './EntryViews/FileEntryView.jsx';

class ProjectTemplateIndex extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/template';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Name","name"),
            {
                header: "Project type",
                key: "projectTypeId",
                headerProps: {className: 'dashboardheader'},
                render: typeId=><ProjectTypeView entryId={typeId}/>
            },
            {
                header: "File",
                key: "fileRef",
                headerProps: {className: 'dashboardheader'},
                render: fileRef=><FileEntryView entryId={fileRef}/>
            },
            this.actionIcons()
        ];
    }

    newElementCallback(event) {
        this.props.history.push("/template/new");
    }
}

export default ProjectTemplateIndex;