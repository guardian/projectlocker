import React from 'react';
import GeneralListComponent from "./GeneralListComponent.jsx";
import WorkingGroupEntryView from "./EntryViews/WorkingGroupEntryView.jsx";
import axios from "axios";

class CommissionsList extends GeneralListComponent {
    constructor(props) {
        super(props);

        this.endpoint = "/api/pluto/commission";
        this.filterEndpoint = "/api/pluto/commission/list";

        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            {
                header: "Working group",
                key: "workingGroupId",
                render: typeId=><WorkingGroupEntryView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Title","title"),
            GeneralListComponent.standardColumn("Status","status"),
            GeneralListComponent.dateTimeColumn("Created", "created"),
            this.actionIcons()
        ];

    }

    dependenciesDidLoad(){
        this.setState({filterTerms: this.props.location.search.includes("mine") ? {user: this.state.uid, match: "W_EXACT"} : {match: "W_CONTAINS"}})
    }

    newElementCallback(event) {
        this.props.history.push("/commission/new");
    }
}

export default CommissionsList;