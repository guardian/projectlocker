import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';
import ProjectEntryFiles from './ProjectEntryFiles.jsx';
import ProjectTypeView from './ProjectTypeView.jsx';

class ProjectEntryList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/project';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Title","title"),
            {
                header: "Pluto project",
                key: "vidispineId",
                render: vsid=><a target="_blank" href={this.props.plutoBaseUrl + "/" + vsid}>{vsid}</a>,
                headerProps: { className: 'dashboardheader'}
            },
            {
                header: "Files",
                key: "id",
                render: (itemid)=><ProjectEntryFiles entryId={itemid}/>,
                headerProps: { className: 'dashboardheader'}
            },
            {
                header: "Project type",
                key: "projectTypeId",
                render: (typeId)=><ProjectTypeView projectType={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Created", "created"),
            GeneralListComponent.standardColumn("Owner","user"),
        ];
    }

    gotDataCallback(result){
        super.gotDataCallback(result);
    }
}

export default ProjectEntryList;