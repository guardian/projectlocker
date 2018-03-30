import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';
import PlutoSubtypeEntryView from './EntryViews/PlutoSubtypeEntryView.jsx';

class ProjectTypeList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/projecttype';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Name","name"),
            GeneralListComponent.standardColumn("Opens with", "opensWith"),
            GeneralListComponent.standardColumn("Target version", "targetVersion"),
            GeneralListComponent.standardColumn("File extension", "fileExtension"),
            {
                header: "Pluto type",
                key: "plutoType",
                headerProps: {className: 'dashboardheader'},
                render: subtyperef=><PlutoSubtypeEntryView entryId={subtyperef}/>
            },
            this.actionIcons()
        ];
    }

    newElementCallback(event) {
        this.props.history.push("/type/new");
    }
}

export default ProjectTypeList;